package com.example.webcrawler.controller;

import com.example.webcrawler.model.CrawlRequest;
import com.example.webcrawler.model.CrawlResponse;
import com.example.webcrawler.service.CrawlerService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class CrawlerController {

    private static final String VISITED_KEY = "crawler:visited";
    private static final String DISCOVERED_KEY = "crawler:discovered";

    private final CrawlerService crawlerService;
    private final StringRedisTemplate redisTemplate;

    public CrawlerController(CrawlerService crawlerService, StringRedisTemplate redisTemplate) {
        this.crawlerService = crawlerService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/crawl")
    public ResponseEntity<CrawlResponse> crawl(@RequestBody CrawlRequest request) {
        if (request == null || request.getSeedUrl() == null || request.getSeedUrl().isBlank()) {
            return ResponseEntity.badRequest().body(CrawlResponse.error(null, "seedUrl is required"));
        }

        CrawlResponse response = crawlerService.crawl(request);
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/visited")
    public ResponseEntity<Set<String>> getVisited() {
        Set<String> visited = redisTemplate.opsForSet().members(VISITED_KEY);
        return ResponseEntity.ok(visited);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            Long visitedCount = redisTemplate.opsForSet().size(VISITED_KEY);
            Long discoveredCount = redisTemplate.opsForSet().size(DISCOVERED_KEY);
            status.put("redisConnected", true);
            status.put("visitedCount", visitedCount == null ? 0 : visitedCount);
            status.put("discoveredCount", discoveredCount == null ? 0 : discoveredCount);
        } catch (Exception e) {
            status.put("redisConnected", false);
            status.put("message", "Could not reach Redis: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(status);
        }
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        redisTemplate.delete(VISITED_KEY);
        redisTemplate.delete(DISCOVERED_KEY);
        return ResponseEntity.ok(Map.of("status", "RESET", "message", "Crawler state cleared"));
    }
}
