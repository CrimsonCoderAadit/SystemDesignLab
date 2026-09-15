package com.example.webcrawler.model;

/**
 * Request body for POST /api/crawl.
 * Example:
 * {
 *   "seedUrl": "https://example.com",
 *   "maxPages": 10
 * }
 */
public class CrawlRequest {

    private String seedUrl;

    /** Optional. Null/absent means "use the configured default". */
    private Integer maxPages;

    /** Optional. Allows crawling outside the seed's own host. Defaults to false. */
    private boolean crawlExternalLinks = false;

    public CrawlRequest() {
    }

    public CrawlRequest(String seedUrl, Integer maxPages) {
        this.seedUrl = seedUrl;
        this.maxPages = maxPages;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public Integer getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }

    public boolean isCrawlExternalLinks() {
        return crawlExternalLinks;
    }

    public void setCrawlExternalLinks(boolean crawlExternalLinks) {
        this.crawlExternalLinks = crawlExternalLinks;
    }
}
