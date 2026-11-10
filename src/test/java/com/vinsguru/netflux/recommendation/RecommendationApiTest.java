package com.vinsguru.netflux.recommendation;

import com.vinsguru.netflux.recommendation.config.MovieServiceProperties;
import com.vinsguru.netflux.recommendation.dto.HomeResponse;
import com.vinsguru.netflux.recommendation.dto.MovieSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@AutoConfigureMockRestServiceServer
class RecommendationApiTest {

    private static final String EMPTY_LIST = "[]";

    @Autowired
    RestTestClient restTestClient;

    @Autowired
    MockRestServiceServer mockServer;

    @Autowired
    MovieServiceProperties movieServiceProperties;

    @Test
    @DisplayName("Should return home page with all sections populated")
    void shouldReturnHomeWithAllSections() {
        mockServer.expect(requestTo(containsString("/api/users/watchlist")))
                .andExpect(header("X-User-Id", "42"))
                .andRespond(withSuccess("[2, 1]", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("/api/movies/batch?ids=2,1")))
                .andRespond(withSuccess(movies(1, 2), MediaType.APPLICATION_JSON));
        expectSections(movies(10), movies(20), genre -> movies(30));

        var response = getHome("42");

        assertThat(ids(response.watchlist())).containsExactly(2L, 1L);
        assertThat(ids(response.popular())).containsExactly(10L);
        assertThat(ids(response.latest())).containsExactly(20L);
        assertThat(response.byGenre()).containsOnlyKeys(movieServiceProperties.genres());
        assertThat(response.byGenre().values())
                .allSatisfy(movies -> assertThat(ids(movies)).containsExactly(30L));
        assertThat(response.watchlist().getFirst().title()).isEqualTo("Movie 2");
        assertThat(response.watchlist().getFirst().genres()).containsExactly("Drama");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should keep watchlist in user-service id order")
    void shouldPreserveWatchlistOrder() {
        expectWatchlist("[3, 1, 2]");
        mockServer.expect(requestTo(containsString("/api/movies/batch?ids=3,1,2")))
                .andRespond(withSuccess(movies(1, 2, 3), MediaType.APPLICATION_JSON));
        expectSections(EMPTY_LIST, EMPTY_LIST, genre -> EMPTY_LIST);

        var response = getHome("42");

        assertThat(ids(response.watchlist())).containsExactly(3L, 1L, 2L);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should drop watchlist ids missing from batch response")
    void shouldDropWatchlistIdsMissingFromBatch() {
        expectWatchlist("[1, 2]");
        mockServer.expect(requestTo(containsString("/api/movies/batch?ids=1,2")))
                .andRespond(withSuccess(movies(1), MediaType.APPLICATION_JSON));
        expectSections(EMPTY_LIST, EMPTY_LIST, genre -> EMPTY_LIST);

        var response = getHome("42");

        assertThat(ids(response.watchlist())).containsExactly(1L);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should skip batch call when watchlist is empty")
    void shouldSkipBatchCallWhenWatchlistEmpty() {
        expectWatchlist(EMPTY_LIST);
        expectSections(movies(10), movies(20), genre -> EMPTY_LIST);

        var response = getHome("42");

        assertThat(response.watchlist()).isEmpty();
        assertThat(ids(response.popular())).containsExactly(10L);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should keep all genres in fixed order when searches return no results")
    void shouldKeepEmptyGenresInFixedOrder() {
        expectWatchlist(EMPTY_LIST);
        expectSections(EMPTY_LIST, EMPTY_LIST, genre -> EMPTY_LIST);

        var response = getHome("42");

        assertThat(response.byGenre().keySet()).containsExactlyElementsOf(movieServiceProperties.genres());
        assertThat(response.byGenre().values()).allSatisfy(movies -> assertThat(movies).isEmpty());
        mockServer.verify();
    }

    @Test
    @DisplayName("Should URI-encode genre names containing spaces")
    void shouldEncodeGenreNamesWithSpaces() {
        expectWatchlist(EMPTY_LIST);
        expectSections(EMPTY_LIST, EMPTY_LIST, genre -> switch (genre) {
            case "Science Fiction" -> movies(40);
            case "TV Movie" -> movies(50);
            default -> EMPTY_LIST;
        });

        var response = getHome("42");

        assertThat(ids(response.byGenre().get("Science Fiction"))).containsExactly(40L);
        assertThat(ids(response.byGenre().get("TV Movie"))).containsExactly(50L);
        mockServer.verify();
    }

    @Test
    @DisplayName("Should return empty list when upstream body is empty")
    void shouldReturnEmptyListWhenUpstreamBodyEmpty() {
        expectWatchlist(EMPTY_LIST);
        mockServer.expect(requestTo(containsString("/api/movies/popular")))
                .andRespond(withSuccess());
        mockServer.expect(requestTo(containsString("/api/movies/latest")))
                .andRespond(withSuccess(EMPTY_LIST, MediaType.APPLICATION_JSON));
        expectGenres(genre -> EMPTY_LIST);

        var response = getHome("42");

        assertThat(response.popular()).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("Should return 401 ProblemDetail when X-User-Id header is missing")
    void shouldReturn401WhenUserIdMissing() {
        var problem = restTestClient.get()
                .uri("/api/recommendations/home")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(401);
        assertThat(problem.getTitle()).isEqualTo("Unauthorized");
        mockServer.verify();
    }

    @Test
    @DisplayName("Should return 400 ProblemDetail when X-User-Id is not numeric")
    void shouldReturn400WhenUserIdNotNumeric() {
        var problem = restTestClient.get()
                .uri("/api/recommendations/home")
                .header("X-User-Id", "abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult()
                .getResponseBody();

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getTitle()).isEqualTo("Bad Request");
        mockServer.verify();
    }

    private HomeResponse getHome(String userId) {
        return restTestClient.get()
                .uri("/api/recommendations/home")
                .header("X-User-Id", userId)
                .exchange()
                .expectStatus().isOk()
                .expectBody(HomeResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private void expectWatchlist(String body) {
        mockServer.expect(requestTo(containsString("/api/users/watchlist")))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void expectSections(String popular, String latest, Function<String, String> genreBody) {
        var limit = movieServiceProperties.limit();
        mockServer.expect(requestTo(containsString("/api/movies/popular?limit=" + limit)))
                .andRespond(withSuccess(popular, MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(containsString("/api/movies/latest?limit=" + limit)))
                .andRespond(withSuccess(latest, MediaType.APPLICATION_JSON));
        expectGenres(genreBody);
    }

    private void expectGenres(Function<String, String> genreBody) {
        var limit = movieServiceProperties.limit();
        movieServiceProperties.genres()
                .forEach(genre -> mockServer.expect(requestTo(containsString(
                                "/api/movies/search?genre=" + genre.replace(" ", "%20") + "&limit=" + limit)))
                        .andRespond(withSuccess(genreBody.apply(genre), MediaType.APPLICATION_JSON)));
    }

    private static String movies(long... ids) {
        return LongStream.of(ids)
                .mapToObj(RecommendationApiTest::movie)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String movie(long id) {
        return """
                {"id": %d, "title": "Movie %d", "posterPath": "/p%d.jpg", "backdropPath": "/b%d.jpg",
                 "voteAverage": 7.5, "releaseDate": "2024-01-15", "genres": ["Drama"]}
                """.formatted(id, id, id, id);
    }

    private static List<Long> ids(List<MovieSummary> movies) {
        return movies.stream()
                .map(MovieSummary::id)
                .toList();
    }
}
