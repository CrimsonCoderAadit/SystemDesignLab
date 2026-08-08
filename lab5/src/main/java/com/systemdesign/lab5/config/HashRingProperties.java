package com.systemdesign.lab5.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.hash-ring")
public class HashRingProperties {

    private int virtualNodes = 150;

    private List<String> initialNodes = new ArrayList<>();
}
