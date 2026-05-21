package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class MoviesHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {

        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {

            byte[] bytes =
                    "[]".getBytes(StandardCharsets.UTF_8);

            ex.getResponseHeaders().set(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            ex.sendResponseHeaders(200, bytes.length);

            OutputStream os =
                    ex.getResponseBody();

            os.write(bytes);

            os.close();
        }
    }
}