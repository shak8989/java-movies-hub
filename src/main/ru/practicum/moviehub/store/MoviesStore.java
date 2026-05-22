package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;

public class MoviesStore {
    private final Map<Long, Movie> movies = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized Movie add(String title, int year) {
        Movie movie = new Movie(nextId++, title, year);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public synchronized List<Movie> findAll() {
        return new ArrayList<>(movies.values());
    }

    public synchronized Optional<Movie> findById(long id) {
        return Optional.ofNullable(movies.get(id));
    }

    public synchronized boolean delete(long id) {
        return movies.remove(id) != null;
    }

    public synchronized List<Movie> findByYear(int year) {
        return movies.values()
                .stream()
                .filter(movie -> movie.getYear() == year)
                .toList();
    }

    public synchronized void clear() {
        movies.clear();
        nextId = 1;
    }
}