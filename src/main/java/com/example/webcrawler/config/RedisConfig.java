package com.example.webcrawler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis is used only for the two pieces of state that must survive across
 * requests and be visible to concurrent crawl calls: the visited-URL set and
 * the discovered-URL set. A plain StringRedisTemplate is enough since every
 * value we store is a URL string - there is no need for JSON/object
 * serialization here, which also avoids common Redis (de)serialization
 * pitfalls with default Spring converters.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
