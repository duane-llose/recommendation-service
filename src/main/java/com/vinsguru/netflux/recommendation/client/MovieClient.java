package com.vinsguru.netflux.recommendation.client;

import com.vinsguru.netflux.recommendation.dto.MovieSummary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MovieClient {

    private static final ParameterizedTypeReference<List<MovieSummary>> MOVIE_LIST = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public MovieClient(RestClient movieRestClient) {
        this.restClient = movieRestClient;
    }

    public List<MovieSummary> getPopular(int limit) {
        return fetch(restClient.get().uri("/api/movies/popular?limit={limit}", limit));
    }

    public List<MovieSummary> getLatest(int limit) {
        return fetch(restClient.get().uri("/api/movies/latest?limit={limit}", limit));
    }

    public List<MovieSummary> searchByGenre(String genre, int limit) {
        return fetch(restClient.get().uri("/api/movies/search?genre={genre}&limit={limit}", genre, limit));
    }

    public List<MovieSummary> getByIds(List<Long> ids) {
        var joinedIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        return fetch(restClient.get().uri(uriBuilder -> uriBuilder.path("/api/movies/batch")
                .queryParam("ids", joinedIds)
                .build()));
    }

    private List<MovieSummary> fetch(RestClient.RequestHeadersSpec<?> request) {
        return Optional.ofNullable(request.retrieve().body(MOVIE_LIST))
                .orElse(List.of());
    }
}
