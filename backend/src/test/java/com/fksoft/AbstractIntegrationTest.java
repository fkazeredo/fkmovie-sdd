package com.fksoft;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests: boots the full application on a random port against a
 * shared Postgres Testcontainer, with one cached Spring context for the whole suite. HTTP
 * helpers take optional {@code "Header-Name", "value"} pairs (e.g. Authorization, Cookie).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    protected HttpResponse<String> get(String path, String... headerPairs) throws IOException, InterruptedException {
        return send(requestBuilder(path, headerPairs).GET());
    }

    protected HttpResponse<String> postJson(String path, String jsonBody, String... headerPairs)
            throws IOException, InterruptedException {
        return send(requestBuilder(path, headerPairs)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)));
    }

    protected HttpResponse<String> putJson(String path, String jsonBody, String... headerPairs)
            throws IOException, InterruptedException {
        return send(requestBuilder(path, headerPairs)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody)));
    }

    protected HttpResponse<String> delete(String path, String... headerPairs) throws IOException, InterruptedException {
        return send(requestBuilder(path, headerPairs).DELETE());
    }

    private HttpRequest.Builder requestBuilder(String path, String... headerPairs) {
        int port = environment.getRequiredProperty("local.server.port", Integer.class);
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        for (int i = 0; i < headerPairs.length; i += 2) {
            builder.header(headerPairs[i], headerPairs[i + 1]);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
