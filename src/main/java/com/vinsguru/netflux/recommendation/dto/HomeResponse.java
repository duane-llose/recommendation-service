package com.vinsguru.netflux.recommendation.dto;

import java.util.List;
import java.util.Map;

public record HomeResponse(List<MovieSummary> watchlist,
                           List<MovieSummary> popular,
                           List<MovieSummary> latest,
                           Map<String, List<MovieSummary>> byGenre) {
}
