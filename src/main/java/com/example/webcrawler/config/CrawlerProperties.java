package com.example.webcrawler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized crawler tuning values, bound from the "crawler.*" prefix in
 * application.properties so they can be overridden via environment
 * variables (e.g. CRAWLER_DEFAULTMAXPAGES) without touching code.
 */
@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    /** Used when the client omits maxPages in the request. */
    private int defaultMaxPages = 10;

    /** Hard ceiling so a lab demo request can never crawl indefinitely. */
    private int maxAllowedPages = 100;

    /** Jsoup connection/read timeout in milliseconds. */
    private int timeoutMs = 5000;

    /** User-Agent string sent with every Jsoup request. */
    private String userAgent = "SystemDesignLab-WebCrawler/1.0";

    /**
     * Development-only escape hatch that allows crawling localhost/private
     * targets, for use with the bundled sample-pages demo site. Must stay
     * false by default: this is what keeps the crawler from being usable
     * as an SSRF tool against internal infrastructure.
     */
    private boolean allowLocalTargets = false;

    public int getDefaultMaxPages() {
        return defaultMaxPages;
    }

    public void setDefaultMaxPages(int defaultMaxPages) {
        this.defaultMaxPages = defaultMaxPages;
    }

    public int getMaxAllowedPages() {
        return maxAllowedPages;
    }

    public void setMaxAllowedPages(int maxAllowedPages) {
        this.maxAllowedPages = maxAllowedPages;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isAllowLocalTargets() {
        return allowLocalTargets;
    }

    public void setAllowLocalTargets(boolean allowLocalTargets) {
        this.allowLocalTargets = allowLocalTargets;
    }
}
