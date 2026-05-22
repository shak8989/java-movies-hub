
package ru.practicum.moviehub.model;
public class Movie {
    private final long id;
    private final String title;
    private final int year;

    public Movie(long id, String title, int year) {
        this.id = id;
        this.title = title;
        this.year = year;
    }

    public long getId() {
        return id;
    }

    public int getYear() {
        return year;
    }

    public String toJson() {
        return "{\"id\":" + id +
                ",\"title\":\"" + escape(title) +
                "\",\"year\":" + year + "}";
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}