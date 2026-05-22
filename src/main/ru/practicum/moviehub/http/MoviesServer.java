
package ru.practicum.moviehub.http;
import ru.practicum.moviehub.store.MoviesStore;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {

    private HttpServer server;
    private final MoviesStore store = new MoviesStore();

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
            server.createContext("/movies", new MoviesHandler(store));
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось запустить сервер", e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    public MoviesStore getStore() {

        return store;
    }
}