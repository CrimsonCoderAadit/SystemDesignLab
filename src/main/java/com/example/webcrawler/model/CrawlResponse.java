package com.example.webcrawler.model;

import java.util.List;

/**
 * Response body returned by POST /api/crawl.
 */
public class CrawlResponse {

    private String seedUrl;
    private int pagesVisited;
    private int urlsDiscovered;
    private List<String> visitedUrls;
    private List<String> failedUrls;
    private long durationMs;
    private String status; // COMPLETED, ERROR
    private String message;

    public CrawlResponse() {
    }

    public static CrawlResponse error(String seedUrl, String message) {
        CrawlResponse response = new CrawlResponse();
        response.setSeedUrl(seedUrl);
        response.setStatus("ERROR");
        response.setMessage(message);
        return response;
    }

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public int getPagesVisited() {
        return pagesVisited;
    }

    public void setPagesVisited(int pagesVisited) {
        this.pagesVisited = pagesVisited;
    }

    public int getUrlsDiscovered() {
        return urlsDiscovered;
    }

    public void setUrlsDiscovered(int urlsDiscovered) {
        this.urlsDiscovered = urlsDiscovered;
    }

    public List<String> getVisitedUrls() {
        return visitedUrls;
    }

    public void setVisitedUrls(List<String> visitedUrls) {
        this.visitedUrls = visitedUrls;
    }

    public List<String> getFailedUrls() {
        return failedUrls;
    }

    public void setFailedUrls(List<String> failedUrls) {
        this.failedUrls = failedUrls;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
