package com.example.urlshortener.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.example.urlshortener.entity.UrlMapping;
import com.example.urlshortener.repository.UrlRepository;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisTemplate;

    public UrlService(UrlRepository urlRepository,
                      StringRedisTemplate redisTemplate) {
        this.urlRepository = urlRepository;
        this.redisTemplate = redisTemplate;
    }

    public String createShortUrl(String longUrl) {

        String hash = generateSHA256(longUrl);

        int length = 6;
        String shortCode = hash.substring(0, length);

        while (true) {

            Optional<UrlMapping> existing =
                    urlRepository.findByShortCode(shortCode);

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

    public Optional<String> getLongUrl(String shortCode) {

        long start = System.nanoTime();

        // 1. Check Redis first
        String cachedUrl = redisTemplate.opsForValue().get(shortCode);

        if (cachedUrl != null) {

            System.out.println("Cache HIT");

            long end = System.nanoTime();

            System.out.println("Response Time: "
                    + ((end - start) / 1_000_000.0) + " ms");

            return Optional.of(cachedUrl);
        }

        // 2. Cache Miss
        System.out.println("Cache MISS");

        Optional<UrlMapping> urlMapping =
                urlRepository.findByShortCode(shortCode);

        if (urlMapping.isPresent()) {

            String longUrl = urlMapping.get().getLongUrl();

            // Store in Redis
            redisTemplate.opsForValue().set(shortCode, longUrl);

            long end = System.nanoTime();

            System.out.println("Response Time: "
                    + ((end - start) / 1_000_000.0) + " ms");

            return Optional.of(longUrl);
        }

        return Optional.empty();
    }

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