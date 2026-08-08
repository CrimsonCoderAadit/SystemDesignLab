package com.systemdesign.lab5.config;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class MongoTemplateConfig {

    private final MongoNodeProperties mongoNodeProperties;

    public MongoTemplateConfig(MongoNodeProperties mongoNodeProperties) {
        this.mongoNodeProperties = mongoNodeProperties;
    }

    @Bean
    @Qualifier("mongoTemplateMap")
    public Map<String, MongoTemplate> mongoTemplateMap() {
        Map<String, MongoTemplate> templates = new LinkedHashMap<>();

        mongoNodeProperties.getNodes().forEach((nodeName, nodeConfig) -> {
            ConnectionString connectionString = new ConnectionString(
                    String.format("mongodb://%s:%d/%s", nodeConfig.getHost(), nodeConfig.getPort(), nodeConfig.getDatabase()));
            MongoClient mongoClient = MongoClients.create(connectionString);
            SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, nodeConfig.getDatabase());
            templates.put(nodeName, new MongoTemplate(factory));
        });

        return templates;
    }
}
