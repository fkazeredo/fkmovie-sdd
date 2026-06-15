package com.fksoft.domain.screening;

import static org.assertj.core.api.Assertions.assertThat;

import com.fksoft.domain.auth.RegistrationIntegrationTestSupport;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** SPEC-0008 acceptance: admin movie CRUD, listing/search, archive, and admin-only access. */
class MovieAdminIntegrationTest extends RegistrationIntegrationTestSupport {

    private static final Pattern ID = Pattern.compile("\"id\":\"([^\"]+)\"");

    @Autowired
    private MovieRepository movies;

    @AfterEach
    void cleanMovies() {
        movies.deleteAll();
    }

    @Test
    void createsAndFetchesMovie() throws Exception {
        var bearer = adminBearer();

        var created = createMovie(bearer, "Alien");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body())
                .contains("\"title\":\"Alien\"")
                .contains("\"status\":\"ACTIVE\"")
                .contains("\"ageRating\":\"A14\"");

        var get = get("/api/admin/movies/" + idOf(created), "Authorization", bearer);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(get.body()).contains("\"title\":\"Alien\"");
    }

    @Test
    void updatesMovie() throws Exception {
        var bearer = adminBearer();
        var id = idOf(createMovie(bearer, "Old Title"));

        var update = putJson(
                "/api/admin/movies/" + id,
                "{\"title\":\"New Title\",\"durationMinutes\":95,\"ageRating\":\"A12\"}",
                "Authorization",
                bearer);

        assertThat(update.statusCode()).isEqualTo(200);
        assertThat(update.body())
                .contains("\"title\":\"New Title\"")
                .contains("\"durationMinutes\":95")
                .contains("\"ageRating\":\"A12\"");
    }

    @Test
    void listFiltersByStatusAndSearch() throws Exception {
        var bearer = adminBearer();
        createMovie(bearer, "Searchable Alien");
        var archivedId = idOf(createMovie(bearer, "Archived Film"));
        postJson("/api/admin/movies/" + archivedId + "/archive", "", "Authorization", bearer);

        var active = get("/api/admin/movies?status=ACTIVE", "Authorization", bearer);
        assertThat(active.body()).contains("\"totalElements\":1").contains("Searchable Alien");

        var search = get("/api/admin/movies?search=alien", "Authorization", bearer);
        assertThat(search.body()).contains("\"totalElements\":1").contains("Searchable Alien");
    }

    @Test
    void archiveHidesFromActiveListThenUnarchiveRestores() throws Exception {
        var bearer = adminBearer();
        var id = idOf(createMovie(bearer, "Toggling"));

        var archive = postJson("/api/admin/movies/" + id + "/archive", "", "Authorization", bearer);
        assertThat(archive.statusCode()).isEqualTo(200);
        assertThat(archive.body()).contains("\"status\":\"ARCHIVED\"");
        assertThat(get("/api/admin/movies?status=ACTIVE", "Authorization", bearer)
                        .body())
                .contains("\"totalElements\":0");

        postJson("/api/admin/movies/" + id + "/unarchive", "", "Authorization", bearer);
        assertThat(get("/api/admin/movies?status=ACTIVE", "Authorization", bearer)
                        .body())
                .contains("\"totalElements\":1");
    }

    @Test
    void deletesMovieWithoutScreenings() throws Exception {
        var bearer = adminBearer();
        var id = idOf(createMovie(bearer, "Deletable"));

        var deleted = delete("/api/admin/movies/" + id, "Authorization", bearer);
        assertThat(deleted.statusCode()).isEqualTo(204);
        assertThat(get("/api/admin/movies/" + id, "Authorization", bearer).statusCode())
                .isEqualTo(404);
    }

    @Test
    void unknownMovieReturns404() throws Exception {
        var bearer = adminBearer();

        var response = get("/api/admin/movies/" + UUID.randomUUID(), "Authorization", bearer);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("\"code\":\"movie.not-found\"");
    }

    @Test
    void rejectsInvalidPayload() throws Exception {
        var bearer = adminBearer();

        var blankTitle = postJson(
                "/api/admin/movies",
                "{\"title\":\"\",\"durationMinutes\":120,\"ageRating\":\"A14\"}",
                "Authorization",
                bearer);
        assertThat(blankTitle.statusCode()).isEqualTo(400);
        assertThat(blankTitle.body()).contains("\"code\":\"validation.error\"");

        var badDuration = postJson(
                "/api/admin/movies",
                "{\"title\":\"Too Long\",\"durationMinutes\":601,\"ageRating\":\"A14\"}",
                "Authorization",
                bearer);
        assertThat(badDuration.statusCode()).isEqualTo(400);
    }

    @Test
    void nonAdminIsForbiddenAndAnonymousIsUnauthorized() throws Exception {
        var customerEmail = uniqueEmail();
        register(customerEmail);
        var customerBearer = "Bearer "
                + accessTokenOf(postJson(
                        "/api/auth/login",
                        "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(customerEmail, PASSWORD)));

        var forbidden = get("/api/admin/movies", "Authorization", customerBearer);
        assertThat(forbidden.statusCode()).isEqualTo(403);
        assertThat(forbidden.body()).contains("\"code\":\"auth.forbidden\"");

        assertThat(get("/api/admin/movies").statusCode()).isEqualTo(401);
    }

    private String adminBearer() throws IOException, InterruptedException {
        var email = uniqueEmail();
        seedAdmin(email);
        return bearerFor(email);
    }

    private HttpResponse<String> createMovie(String bearer, String title) throws IOException, InterruptedException {
        var body =
                """
                {"title":"%s","durationMinutes":117,"ageRating":"A14","synopsis":"In space...","posterUrl":"https://img/x.jpg"}
                """
                        .formatted(title);
        return postJson("/api/admin/movies", body, "Authorization", bearer);
    }

    private String idOf(HttpResponse<String> response) {
        var matcher = ID.matcher(response.body());
        if (!matcher.find()) {
            throw new AssertionError("No id in body: " + response.body());
        }
        return matcher.group(1);
    }
}
