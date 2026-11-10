package com.vinsguru.netflux.recommendation.dto;

import java.time.LocalDate;

public record MovieSummary(Long id,
                           String title,
                           String posterPath,
                           String backdropPath,
                           Double voteAverage,
                           LocalDate releaseDate,
                           String[] genres) {
}
