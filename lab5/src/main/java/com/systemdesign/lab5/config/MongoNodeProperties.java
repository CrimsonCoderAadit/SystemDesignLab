package com.systemdesign.lab5.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.mongo")
public class MongoNodeProperties {

    private Map<String, NodeConfig> nodes = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class NodeConfig {
        private String host;
        private int port;
        private String database;
    }
}
