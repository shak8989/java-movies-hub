
package ru.practicum.moviehub.http;
import org.junit.jupiter.api.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";

    private static MoviesServer server;
    private static HttpClient client;

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void beforeEach() {
        server.getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp = sendGet("/movies");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void getMovies_returnsAddedMovies() throws Exception {
        sendPost("/movies", "{\"title\":\"Interstellar\",\"year\":2014}", true);

        HttpResponse<String> resp = sendGet("/movies");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Interstellar"));
        assertTrue(resp.body().contains("2014"));
    }

    @Test
    void postMovies_withValidData_createsMovie() throws Exception {
        HttpResponse<String> resp =
                sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}", true);

        assertEquals(201, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("\"id\":1"));
        assertTrue(resp.body().contains("\"title\":\"Inception\""));
        assertTrue(resp.body().contains("\"year\":2010"));
    }

    @Test
    void postMovies_withEmptyTitle_returns422() throws Exception {
        HttpResponse<String> resp =
                sendPost("/movies", "{\"title\":\"\",\"year\":2010}", true);

        assertEquals(422, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Ошибка валидации"));
        assertTrue(resp.body().contains("название не должно быть пустым"));
    }

    @Test
    void postMovies_withTooLongTitle_returns422() throws Exception {
        String longTitle = "a".repeat(101);

        HttpResponse<String> resp =
                sendPost("/movies", "{\"title\":\"" + longTitle + "\",\"year\":2010}", true);

        assertEquals(422, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("название должно быть не длиннее 100 символов"));
    }

    @Test
    void postMovies_withInvalidYear_returns422() throws Exception {
        HttpResponse<String> resp =
                sendPost("/movies", "{\"title\":\"Old Movie\",\"year\":1800}", true);

        assertEquals(422, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("год должен быть между"));
    }

    @Test
    void postMovies_withWrongContentType_returns415() throws Exception {
        HttpResponse<String> resp =
                sendPost("/movies", "{\"title\":\"Inception\",\"year\":2010}", false);

        assertEquals(415, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Неподдерживаемый Content-Type"));
    }

    @Test
    void postMovies_withInvalidJson_returns400() throws Exception {
        HttpResponse<String> resp =
                sendPost("/movies", "{bad json", true);

        assertEquals(400, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Некорректный JSON"));
    }

    @Test
    void getMovieById_whenExists_returnsMovie() throws Exception {
        sendPost("/movies", "{\"title\":\"Matrix\",\"year\":1999}", true);

        HttpResponse<String> resp = sendGet("/movies/1");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("\"title\":\"Matrix\""));
    }

    @Test
    void getMovieById_whenNotFound_returns404() throws Exception {
        HttpResponse<String> resp = sendGet("/movies/999");

        assertEquals(404, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void getMovieById_whenIdIsNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = sendGet("/movies/abc");

        assertEquals(400, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void deleteMovie_whenExists_returns204() throws Exception {
        sendPost("/movies", "{\"title\":\"Avatar\",\"year\":2009}", true);

        HttpResponse<String> resp = sendDelete("/movies/1");

        assertEquals(204, resp.statusCode());
        assertJsonContentType(resp);
        assertEquals("", resp.body());
    }

    @Test
    void deleteMovie_whenNotFound_returns404() throws Exception {
        HttpResponse<String> resp = sendDelete("/movies/999");

        assertEquals(404, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Фильм не найден"));
    }

    @Test
    void deleteMovie_whenIdIsNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = sendDelete("/movies/abc");

        assertEquals(400, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Некорректный ID"));
    }

    @Test
    void getMoviesByYear_returnsMoviesOfSelectedYear() throws Exception {
        sendPost("/movies", "{\"title\":\"Movie 1\",\"year\":2010}", true);
        sendPost("/movies", "{\"title\":\"Movie 2\",\"year\":2020}", true);

        HttpResponse<String> resp = sendGet("/movies?year=2010");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Movie 1"));
        assertFalse(resp.body().contains("Movie 2"));
    }

    @Test
    void getMoviesByYear_whenNoMovies_returnsEmptyArray() throws Exception {
        sendPost("/movies", "{\"title\":\"Movie 1\",\"year\":2010}", true);

        HttpResponse<String> resp = sendGet("/movies?year=2020");

        assertEquals(200, resp.statusCode());
        assertJsonContentType(resp);
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returns400() throws Exception {
        HttpResponse<String> resp = sendGet("/movies?year=abc");

        assertEquals(400, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Некорректный параметр запроса"));
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(405, resp.statusCode());
        assertJsonContentType(resp);
        assertTrue(resp.body().contains("Метод не поддерживается"));
    }

    private HttpResponse<String> sendGet(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendDelete(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .DELETE()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> sendPost(
            String path,
            String body,
            boolean correctContentType
    ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));

        if (correctContentType) {
            builder.header("Content-Type", "application/json; charset=UTF-8");
        } else {
            builder.header("Content-Type", "text/plain");
        }

        return client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private void assertJsonContentType(HttpResponse<String> resp) {
        assertEquals(
                "application/json; charset=UTF-8",
                resp.headers().firstValue("Content-Type").orElse("")
        );
    }
}