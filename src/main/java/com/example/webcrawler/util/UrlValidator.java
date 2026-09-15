package com.example.webcrawler.util;

import com.example.webcrawler.config.CrawlerProperties;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Centralizes URL safety and normalization rules so the same logic is used
 * both for the seed URL and for every link discovered on a page.
 *
 * Safety rules (SSRF protection):
 *   - Only http/https schemes are allowed. javascript:, mailto:, tel:, file:
 *     and any other scheme are rejected outright.
 *   - The hostname is resolved and checked against loopback, link-local,
 *     site-local (private), IPv6 unique-local (fc00::/7) and any-local
 *     ("0.0.0.0") address ranges using InetAddress, so both literal IPs
 *     (127.0.0.1, ::1, 169.254.x.x, etc.) and hostnames that resolve to
 *     such addresses (e.g. "localhost") are rejected. CrawlerService also
 *     validates each redirect hop through this same validator BEFORE
 *     connecting to it, so a public URL that redirects to an internal
 *     address is caught before that request is ever made.
 *   - allowLocalTargets (CrawlerProperties) is a development-only override
 *     for the bundled sample-pages demo and defaults to false.
 *
 *   KNOWN LIMITATION (accepted for this lab's scope): this check has a
 *   time-of-check/time-of-use gap against DNS rebinding. The hostname is
 *   resolved here to validate it, but the actual HTTP connection made
 *   later (by Jsoup) re-resolves the same hostname independently; if an
 *   attacker controls DNS for that hostname and returns a public IP for
 *   this check but a private IP moments later for the real connection,
 *   the private address would not be re-validated. Closing this gap fully
 *   requires pinning the validated IP for the actual socket connection
 *   (and, for HTTPS, keeping TLS SNI/hostname verification against the
 *   original hostname) - out of scope for this lab's Jsoup-based fetcher,
 *   but noted here for anyone hardening this for production use.
 *
 * Normalization rules:
 *   - Fragments (#section) are stripped since they identify a location
 *     within a page, not a distinct resource.
 *   - Trailing slash on a bare path is stripped so that
 *     "https://example.com" and "https://example.com/" are treated as the
 *     same page for visited-set purposes.
 *   - Host is lower-cased (hosts are case-insensitive; paths are not).
 */
@Component
public class UrlValidator {

    private final CrawlerProperties properties;

    public UrlValidator(CrawlerProperties properties) {
        this.properties = properties;
    }

    /**
     * Returns a normalized absolute URL string if the URL is well-formed and
     * safe to crawl, or null if it should be discarded.
     */
    public String normalizeAndValidate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return null;
        }

        String candidate = rawUrl.trim();

        // Strip the fragment before parsing so "#top" style links normalize
        // to the same URL as the page they point into.
        int fragmentIndex = candidate.indexOf('#');
        if (fragmentIndex >= 0) {
            candidate = candidate.substring(0, fragmentIndex);
        }
        if (candidate.isBlank()) {
            return null;
        }

        URL url;
        try {
            url = URI.create(candidate).toURL();
        } catch (IllegalArgumentException | MalformedURLException e) {
            return null;
        }

        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) {
            // Rejects javascript:, mailto:, tel:, file:, ftp:, data:, etc.
            return null;
        }

        String host = url.getHost();
        if (host == null || host.isBlank()) {
            return null;
        }

        if (!properties.isAllowLocalTargets() && isUnsafeHost(host)) {
            return null;
        }

        return rebuild(url);
    }

    /**
     * Resolves the host and rejects loopback, private, link-local and
     * wildcard addresses so the crawler cannot be used to reach internal
     * network resources (SSRF protection).
     */
    private boolean isUnsafeHost(String host) {
        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (lowerHost.equals("localhost") || lowerHost.equals("0.0.0.0") || lowerHost.equals("::1")) {
            return true;
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                if (address.isLoopbackAddress()
                        || address.isAnyLocalAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()
                        || isIpv6UniqueLocal(address)) {
                    return true;
                }
            }
            return false;
        } catch (UnknownHostException e) {
            // Cannot resolve -> cannot safely crawl.
            return true;
        }
    }

    /**
     * Checks the IPv6 Unique Local Address range fc00::/7 (RFC 4193).
     * InetAddress.isSiteLocalAddress() only recognizes the deprecated
     * fec0::/10 range for IPv6, so modern ULA addresses (fc00::/7, the
     * IPv6 equivalent of RFC 1918 private IPv4 space) are NOT caught by
     * the isSiteLocalAddress() check above and must be tested explicitly.
     */
    private boolean isIpv6UniqueLocal(InetAddress address) {
        byte[] bytes = address.getAddress();
        return bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC;
    }

    private String rebuild(URL url) {
        String host = IDN.toASCII(url.getHost()).toLowerCase(Locale.ROOT);
        int port = url.getPort();
        String path = url.getPath();
        if (path == null || path.isBlank()) {
            path = "/";
        } else if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String query = url.getQuery();

        StringBuilder sb = new StringBuilder();
        sb.append(url.getProtocol()).append("://").append(host);
        if (port != -1 && port != url.getDefaultPort()) {
            sb.append(':').append(port);
        }
        sb.append(path);
        if (query != null && !query.isBlank()) {
            sb.append('?').append(query);
        }
        return sb.toString();
    }

    /** Extracts the host from an already-normalized URL, used for same-host scope checks. */
    public String extractHost(String normalizedUrl) {
        try {
            return URI.create(normalizedUrl).toURL().getHost().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException | MalformedURLException e) {
            return null;
        }
    }
}
