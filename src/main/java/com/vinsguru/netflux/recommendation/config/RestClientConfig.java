package com.vinsguru.netflux.recommendation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private final RestClient.Builder builder;

    public RestClientConfig(RestClient.Builder builder) {
        this.builder = builder;
    }

    @Bean
    public RestClient movieRestClient(MovieServiceProperties properties) {
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient userRestClient(UserServiceProperties properties) {
        return builder.clone()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
