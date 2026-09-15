package com.example.webcrawler;

import com.example.webcrawler.config.CrawlerProperties;
import com.example.webcrawler.util.UrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UrlValidatorTest {

    private UrlValidator validator;

    @BeforeEach
    void setUp() {
        CrawlerProperties properties = new CrawlerProperties();
        validator = new UrlValidator(properties);
    }

    @Test
    void acceptsValidHttpsUrl() {
        assertEquals("https://example.com/", validator.normalizeAndValidate("https://example.com"));
    }

    @Test
    void stripsFragment() {
        assertEquals("https://example.com/page", validator.normalizeAndValidate("https://example.com/page#section"));
    }

    @Test
    void stripsTrailingSlash() {
        assertEquals("https://example.com/page", validator.normalizeAndValidate("https://example.com/page/"));
    }

    @Test
    void rejectsJavascriptScheme() {
        assertNull(validator.normalizeAndValidate("javascript:alert(1)"));
    }

    @Test
    void rejectsMailto() {
        assertNull(validator.normalizeAndValidate("mailto:test@example.com"));
    }

    @Test
    void rejectsTel() {
        assertNull(validator.normalizeAndValidate("tel:+1234567890"));
    }

    @Test
    void rejectsFileScheme() {
        assertNull(validator.normalizeAndValidate("file:///etc/passwd"));
    }

    @Test
    void rejectsBlank() {
        assertNull(validator.normalizeAndValidate(""));
        assertNull(validator.normalizeAndValidate(null));
    }

    @Test
    void rejectsLocalhostByDefault() {
        assertNull(validator.normalizeAndValidate("http://localhost:8080/api/crawl"));
    }

    @Test
    void rejectsLoopbackIp() {
        assertNull(validator.normalizeAndValidate("http://127.0.0.1/"));
    }

    @Test
    void rejectsMalformedUrl() {
        assertNull(validator.normalizeAndValidate("not a url"));
    }

    @Test
    void extractsHost() {
        assertEquals("example.com", validator.extractHost("https://example.com/page"));
    }
}
