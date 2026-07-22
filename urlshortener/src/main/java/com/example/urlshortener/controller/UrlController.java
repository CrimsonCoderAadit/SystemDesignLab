package com.example.urlshortener.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.example.urlshortener.service.UrlService;

// Controller class that exposes the REST endpoints for shortening and resolving URLs
@RestController
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    // Accepts a long URL and returns a shortened URL
    @PostMapping("/shorten")
    public ResponseEntity<Map<String, String>> shortenUrl(@RequestBody Map<String, String> request) {
        String longUrl = request.get("longUrl");

        String shortCode = urlService.createShortUrl(longUrl);
        String shortUrl = "http://localhost:8080/" + shortCode;

        Map<String, String> response = new HashMap<>();
        response.put("shortUrl", shortUrl);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Redirects the browser from the short code to the original long URL
    @GetMapping("/{shortCode}")
    public RedirectView redirectToLongUrl(@PathVariable String shortCode) {
        Optional<String> longUrl = urlService.getLongUrl(shortCode);

        if (longUrl.isPresent()) {
            return new RedirectView(longUrl.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short code not found");
        }
    }
}
