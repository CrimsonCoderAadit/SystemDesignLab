package com.example.webcrawler.service;

import com.example.webcrawler.config.CrawlerProperties;
import com.example.webcrawler.model.CrawlRequest;
import com.example.webcrawler.model.CrawlResponse;
import com.example.webcrawler.util.UrlValidator;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Core crawling algorithm.
 *
 * Design notes:
 *   - The active crawl queue (ArrayDeque) is local to a single crawl request
 *     and is not shared between requests - this keeps the lab implementation
 *     simple while still allowing multiple crawl requests to run
 *     concurrently, each with its own queue and its own bounded page budget.
 *   - The visited-URL set is GLOBAL and lives in Redis under the key
 *     "crawler:visited". This is intentional: it is what lets a second
 *     POST /api/crawl (with the same or overlapping seed) demonstrate that
 *     already-visited pages are skipped instead of being re-fetched, and it
 *     is what makes concurrent requests safe - two requests racing to crawl
 *     the same URL will only have one of them actually fetch it, because the
 *     Redis SADD claim below is atomic.
 *   - The discovered-URL set ("crawler:discovered") simply records every
 *     unique valid URL ever seen, for reporting/demonstration purposes.
 */
@Service
public class CrawlerService {

    private static final Logger log = LoggerFactory.getLogger(CrawlerService.class);

    private static final String VISITED_KEY = "crawler:visited";
    private static final String DISCOVERED_KEY = "crawler:discovered";

    private final StringRedisTemplate redisTemplate;
    private final UrlValidator urlValidator;
    private final CrawlerProperties properties;

    public CrawlerService(StringRedisTemplate redisTemplate, UrlValidator urlValidator, CrawlerProperties properties) {
        this.redisTemplate = redisTemplate;
        this.urlValidator = urlValidator;
        this.properties = properties;
    }

    public CrawlResponse crawl(CrawlRequest request) {
        long startTime = System.currentTimeMillis();

        String normalizedSeed = urlValidator.normalizeAndValidate(request.getSeedUrl());
        if (normalizedSeed == null) {
            return CrawlResponse.error(request.getSeedUrl(), "Invalid seed URL");
        }

        int maxPages = resolveMaxPages(request.getMaxPages());
        String seedHost = urlValidator.extractHost(normalizedSeed);
        boolean crawlExternal = request.isCrawlExternalLinks();

        Deque<String> queue = new ArrayDeque<>();
        // Local guard so a URL discovered twice within the SAME crawl is only
        // ever enqueued once, avoiding duplicate queue entries; the Redis
        // visited set is the authority for "has this ever been crawled".
        Set<String> queuedInThisRun = new HashSet<>();

        queue.add(normalizedSeed);
        queuedInThisRun.add(normalizedSeed);

        List<String> visitedUrls = new ArrayList<>();
        List<String> failedUrls = new ArrayList<>();
        Set<String> discoveredThisRun = new HashSet<>();

        int pagesVisited = 0;

        while (!queue.isEmpty() && pagesVisited < maxPages) {
            String currentUrl = queue.poll();

            // Atomically claim the URL: SADD returns 1 only for the caller
            // that actually adds it, so concurrent crawl requests can never
            // both "win" the same URL. This is the check-then-mark step done
            // as a single atomic Redis operation instead of a separate
            // SISMEMBER followed by SADD, which would be racy.
            boolean claimed = claimUrl(currentUrl);
            if (!claimed) {
                // Already visited by this or another crawl - skip without
                // fetching it again.
                continue;
            }

            try {
                Document document = fetchPage(currentUrl);
                pagesVisited++;
                visitedUrls.add(currentUrl);

                Elements links = document.select("a[href]");
                for (Element link : links) {
                    String absoluteUrl = link.absUrl("href");
                    String normalizedLink = urlValidator.normalizeAndValidate(absoluteUrl);
                    if (normalizedLink == null) {
                        continue;
                    }

                    redisTemplate.opsForSet().add(DISCOVERED_KEY, normalizedLink);
                    discoveredThisRun.add(normalizedLink);

                    String linkHost = urlValidator.extractHost(normalizedLink);
                    boolean sameHost = seedHost != null && seedHost.equalsIgnoreCase(linkHost);
                    if (!sameHost && !crawlExternal) {
                        // Recorded as discovered above, but not queued for
                        // crawling - keeps the crawl scoped to the seed's
                        // own site by default.
                        continue;
                    }

                    if (queuedInThisRun.add(normalizedLink)) {
                        queue.add(normalizedLink);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to crawl {}: {}", currentUrl, e.getMessage());
                failedUrls.add(currentUrl);
            }
        }

        CrawlResponse response = new CrawlResponse();
        response.setSeedUrl(normalizedSeed);
        response.setPagesVisited(pagesVisited);
        response.setUrlsDiscovered(discoveredThisRun.size());
        response.setVisitedUrls(visitedUrls);
        response.setFailedUrls(failedUrls);
        response.setStatus("COMPLETED");
        response.setDurationMs(System.currentTimeMillis() - startTime);
        return response;
    }

    /**
     * Atomic claim-if-absent: returns true only if this call is the one that
     * added the URL to the visited set (i.e. it was not already present).
     */
    private boolean claimUrl(String url) {
        Long added = redisTemplate.opsForSet().add(VISITED_KEY, url);
        return added != null && added == 1L;
    }

    private int resolveMaxPages(Integer requested) {
        if (requested == null || requested <= 0) {
            return properties.getDefaultMaxPages();
        }
        return Math.min(requested, properties.getMaxAllowedPages());
    }

    private static final int MAX_REDIRECTS = 5;

    /**
     * Fetches a page, following redirects manually (one hop at a time)
     * instead of letting Jsoup auto-follow them. Auto-follow would connect
     * to the redirect target BEFORE we get a chance to validate it, so a
     * public URL could redirect to an internal address and the crawler
     * would already have made that request by the time any check ran.
     * Validating each hop's target before connecting to it closes that gap.
     */
    private Document fetchPage(String url) throws java.io.IOException {
        String currentUrl = url;

        for (int redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount++) {
            Connection connection = Jsoup.connect(currentUrl)
                    .userAgent(properties.getUserAgent())
                    .timeout(properties.getTimeoutMs())
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(false);

            Connection.Response response = connection.execute();
            int status = response.statusCode();

            if (status >= 300 && status < 400 && response.hasHeader("Location")) {
                String redirectTarget = response.header("Location");
                // Resolve relative Location headers against the URL we just requested.
                String absoluteTarget = java.net.URI.create(currentUrl).resolve(redirectTarget).toString();
                String validatedTarget = urlValidator.normalizeAndValidate(absoluteTarget);
                if (validatedTarget == null) {
                    throw new java.io.IOException("Redirect target rejected by URL safety policy: " + absoluteTarget);
                }
                currentUrl = validatedTarget;
                continue;
            }

            if (status >= 400) {
                throw new java.io.IOException("HTTP error " + status + " fetching " + currentUrl);
            }

            return response.parse();
        }

        throw new java.io.IOException("Too many redirects fetching " + url);
    }
}
