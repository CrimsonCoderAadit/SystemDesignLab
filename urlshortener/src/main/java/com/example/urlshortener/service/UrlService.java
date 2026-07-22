package com.example.urlshortener.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.repository.UrlRepository;

// Service class that contains the logic for creating and resolving short URLs
@Service
public class UrlService {

    private final UrlRepository urlRepository;

    public UrlService(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    // Creates a short code for the given long URL and saves it in the database
    public String createShortUrl(String longUrl) {
        String hash = generateSHA256(longUrl);

        int length = 6;
        String shortCode = hash.substring(0, length);

        // Keep increasing the length of the code until we find one that is not used
        while (true) {
            Optional<UrlMapping> existing = urlRepository.findByShortCode(shortCode);
            if (existing.isEmpty()) {
                break;
            }
            length++;
            shortCode = hash.substring(0, length);
        }

        UrlMapping urlMapping = new UrlMapping(longUrl, shortCode);
        urlRepository.save(urlMapping);

        return shortCode;
    }

    // Finds the original long URL for a given short code
    public Optional<String> getLongUrl(String shortCode) {
        Optional<UrlMapping> urlMapping = urlRepository.findByShortCode(shortCode);
        return urlMapping.map(UrlMapping::getLongUrl);
    }

    // Generates a SHA-256 hash of the input string and returns it as a hex string
    private String generateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
