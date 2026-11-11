package com.vinsguru.netflux.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.movie-service")
public record MovieServiceProperties(String baseUrl,
                                     int limit,
                                     List<String> genres) {
}
