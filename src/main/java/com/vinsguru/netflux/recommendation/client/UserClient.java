package com.vinsguru.netflux.recommendation.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
public class UserClient {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RestClient restClient;

    public UserClient(RestClient userRestClient) {
        this.restClient = userRestClient;
    }

    public List<Long> getWatchlist(Long userId) {
        var ids = restClient.get()
                .uri("/api/users/watchlist")
                .header(USER_ID_HEADER, String.valueOf(userId))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Long>>() {});
        return Optional.ofNullable(ids)
                .orElse(List.of());
    }
}
