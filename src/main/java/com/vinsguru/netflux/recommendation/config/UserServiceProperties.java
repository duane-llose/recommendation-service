package com.vinsguru.netflux.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.user-service")
public record UserServiceProperties(String baseUrl) {
}
