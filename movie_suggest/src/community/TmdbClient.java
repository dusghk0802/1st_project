package community;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/** Server-side TMDB API client using only the Java 11 standard library. */
public final class TmdbClient {
    private static final String API_BASE = "https://api.themoviedb.org/3";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String accessToken;

    public TmdbClient() {
        accessToken = value("TMDB_ACCESS_TOKEN");
        if (accessToken.isBlank()) throw new IllegalStateException("TMDB_ACCESS_TOKEN is not configured.");
    }

    /** Returns the original TMDB JSON response so a repository can map/cache it in the local DB. */
    public String searchMovies(String query, int page) throws IOException, InterruptedException {
        if (query == null || query.isBlank()) throw new IllegalArgumentException("Movie query is required.");
        String parameters = "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&language=ko-KR&page=" + Math.max(1, page);
        return get("/search/movie?" + parameters);
    }

    public String popularMovies(int page) throws IOException, InterruptedException {
        return get("/movie/popular?language=ko-KR&page=" + Math.max(1, page));
    }

    public String movieReviews(long tmdbId, int page) throws IOException, InterruptedException {
        return get("/movie/" + tmdbId + "/reviews?language=ko-KR&page=" + Math.max(1, page));
    }

    public String movieDetails(long tmdbId, String language) throws IOException, InterruptedException {
        String safeLanguage = language == null || language.isBlank() ? "en-US" : language;
        return get("/movie/" + tmdbId + "?language=" + URLEncoder.encode(safeLanguage, StandardCharsets.UTF_8));
    }

    public String movieRecommendations(long tmdbId, int page) throws IOException, InterruptedException {
        return get("/movie/" + tmdbId + "/recommendations?language=ko-KR&page=" + Math.max(1, page));
    }

    public static String posterUrl(String posterPath) {
        return posterPath == null || posterPath.isBlank() ? null : "https://image.tmdb.org/t/p/w500" + posterPath;
    }

    private String get(String pathAndQuery) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE + pathAndQuery))
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) throw new IOException("TMDB API request failed: HTTP " + response.statusCode());
        return response.body();
    }

    private static String value(String key) {
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) return property;
        String environment = System.getenv(key);
        return environment == null ? "" : environment.trim();
    }
}
