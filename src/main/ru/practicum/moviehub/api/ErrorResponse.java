
package ru.practicum.moviehub.api;
import java.util.List;
import java.util.stream.Collectors;

public class ErrorResponse {

    public static String of(String error) {
        return "{\"error\":\"" + escape(error) + "\"}";
    }

    public static String validation(List<String> details) {
        String jsonDetails = details.stream()
                .map(detail -> "\"" + escape(detail) + "\"")
                .collect(Collectors.joining(",", "[", "]"));

        return "{\"error\":\"Ошибка валидации\",\"details\":" + jsonDetails + "}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}