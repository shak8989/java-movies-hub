
package ru.practicum.moviehub.http;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        try {
            if (method.equalsIgnoreCase("GET") && path.equals("/movies")) {
                handleGetMovies(ex, query);
                return;
            }

            if (method.equalsIgnoreCase("POST") && path.equals("/movies")) {
                handlePostMovie(ex);
                return;
            }

            if (path.startsWith("/movies/")) {
                String idText = path.substring("/movies/".length());

                if (method.equalsIgnoreCase("GET")) {
                    handleGetMovieById(ex, idText);
                    return;
                }

                if (method.equalsIgnoreCase("DELETE")) {
                    handleDeleteMovie(ex, idText);
                    return;
                }
            }

            if (
                    method.equalsIgnoreCase("GET") ||
                            method.equalsIgnoreCase("POST") ||
                            method.equalsIgnoreCase("DELETE")
            ) {
                sendJson(ex, 404, ErrorResponse.of("Эндпоинт не найден"));
                return;
            }

            sendJson(ex, 405, ErrorResponse.of("Метод не поддерживается"));

        } catch (Exception e) {
            sendJson(ex, 400, ErrorResponse.of("Некорректный запрос"));
        }
    }

    private void handleGetMovies(HttpExchange ex, String query) throws IOException {
        List<Movie> movies;

        if (query == null || query.isBlank()) {
            movies = store.findAll();
        } else if (query.startsWith("year=")) {
            String yearText = query.substring("year=".length());

            try {
                int year = Integer.parseInt(yearText);
                movies = store.findByYear(year);
            } catch (NumberFormatException e) {
                sendJson(ex, 400, ErrorResponse.of("Некорректный параметр запроса — 'year'"));
                return;
            }
        } else {
            sendJson(ex, 400, ErrorResponse.of("Некорректный параметр запроса"));
            return;
        }

        sendJson(ex, 200, moviesToJson(movies));
    }

    private void handlePostMovie(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders()
                .getFirst("Content-Type");

        if (contentType == null || !contentType.equalsIgnoreCase(CT_JSON)) {
            sendJson(ex, 415, ErrorResponse.of("Неподдерживаемый Content-Type"));
            return;
        }

        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        ParsedMovie parsed;

        try {
            parsed = parseMovie(body);
        } catch (Exception e) {
            sendJson(ex, 400, ErrorResponse.of("Некорректный JSON"));
            return;
        }

        List<String> errors = validate(parsed);

        if (!errors.isEmpty()) {
            sendJson(ex, 422, ErrorResponse.validation(errors));
            return;
        }

        Movie movie = store.add(parsed.title, parsed.year);
        sendJson(ex, 201, movie.toJson());
    }

    private void handleGetMovieById(HttpExchange ex, String idText) throws IOException {
        long id;

        try {
            id = Long.parseLong(idText);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, ErrorResponse.of("Некорректный ID"));
            return;
        }

        store.findById(id)
                .ifPresentOrElse(
                        movie -> {
                            try {
                                sendJson(ex, 200, movie.toJson());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        () -> {
                            try {
                                sendJson(ex, 404, ErrorResponse.of("Фильм не найден"));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                );
    }

    private void handleDeleteMovie(HttpExchange ex, String idText) throws IOException {
        long id;

        try {
            id = Long.parseLong(idText);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, ErrorResponse.of("Некорректный ID"));
            return;
        }

        if (store.delete(id)) {
            sendNoContent(ex);
        } else {
            sendJson(ex, 404, ErrorResponse.of("Фильм не найден"));
        }
    }

    private String moviesToJson(List<Movie> movies) {
        return movies.stream()
                .map(Movie::toJson)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<String> validate(ParsedMovie movie) {
        List<String> errors = new ArrayList<>();

        int maxYear = Year.now().getValue() + 1;

        if (movie.title == null || movie.title.isBlank()) {
            errors.add("название не должно быть пустым");
        }

        if (movie.title != null && movie.title.length() > 100) {
            errors.add("название должно быть не длиннее 100 символов");
        }

        if (movie.year < 1888 || movie.year > maxYear) {
            errors.add("год должен быть между 1888 и " + maxYear);
        }

        return errors;
    }

    private ParsedMovie parseMovie(String json) {
        if (json == null || !json.trim().startsWith("{") || !json.trim().endsWith("}")) {
            throw new IllegalArgumentException();
        }

        String title = extractString(json, "title");
        int year = extractInt(json, "year");

        return new ParsedMovie(title, year);
    }

    private String extractString(String json, String field) {
        String pattern = "\"" + field + "\"";
        int fieldIndex = json.indexOf(pattern);

        if (fieldIndex == -1) {
            throw new IllegalArgumentException();
        }

        int colonIndex = json.indexOf(":", fieldIndex);
        int firstQuote = json.indexOf("\"", colonIndex + 1);
        int secondQuote = json.indexOf("\"", firstQuote + 1);

        if (colonIndex == -1 || firstQuote == -1 || secondQuote == -1) {
            throw new IllegalArgumentException();
        }

        return json.substring(firstQuote + 1, secondQuote);
    }

    private int extractInt(String json, String field) {
        String pattern = "\"" + field + "\"";
        int fieldIndex = json.indexOf(pattern);

        if (fieldIndex == -1) {
            throw new IllegalArgumentException();
        }

        int colonIndex = json.indexOf(":", fieldIndex);

        if (colonIndex == -1) {
            throw new IllegalArgumentException();
        }

        int start = colonIndex + 1;

        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;

        while (end < json.length() &&
                (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        return Integer.parseInt(json.substring(start, end));
    }

    private static class ParsedMovie {
        private final String title;
        private final int year;

        private ParsedMovie(String title, int year) {
            this.title = title;
            this.year = year;
        }
    }
}