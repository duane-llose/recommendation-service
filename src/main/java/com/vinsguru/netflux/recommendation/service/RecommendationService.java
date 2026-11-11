package com.vinsguru.netflux.recommendation.service;

import com.vinsguru.netflux.recommendation.client.MovieClient;
import com.vinsguru.netflux.recommendation.client.UserClient;
import com.vinsguru.netflux.recommendation.config.MovieServiceProperties;
import com.vinsguru.netflux.recommendation.dto.HomeResponse;
import com.vinsguru.netflux.recommendation.dto.MovieSummary;
import com.vinsguru.netflux.recommendation.exceptions.MissingUserIdException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final MovieClient movieClient;
    private final UserClient userClient;
    private final MovieServiceProperties movieServiceProperties;

    public RecommendationService(MovieClient movieClient,
                                 UserClient userClient,
                                 MovieServiceProperties movieServiceProperties) {
        this.movieClient = movieClient;
        this.userClient = userClient;
        this.movieServiceProperties = movieServiceProperties;
    }

    public HomeResponse getHome(Long userId) {
        if (Objects.isNull(userId)) {
            throw new MissingUserIdException();
        }
        var limit = movieServiceProperties.limit();
        return new HomeResponse(
                getWatchlist(userId),
                movieClient.getPopular(limit),
                movieClient.getLatest(limit),
                getByGenre(limit)
        );
    }

    private List<MovieSummary> getWatchlist(Long userId) {
        var watchlistIds = userClient.getWatchlist(userId);
        if (watchlistIds.isEmpty()) {
            return List.of();
        }
        var moviesById = movieClient.getByIds(watchlistIds)
                .stream()
                .collect(Collectors.toMap(MovieSummary::id, Function.identity(), (a, b) -> a));
        return watchlistIds.stream()
                .filter(moviesById::containsKey)
                .map(moviesById::get)
                .toList();
    }

    private Map<String, List<MovieSummary>> getByGenre(int limit) {
        return movieServiceProperties.genres()
                .stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        genre -> movieClient.searchByGenre(genre, limit),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }
}
