package project;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Server-side TMDB client. The token never reaches the browser. */
final class TmdbSimilarClient {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    boolean configured() {
        return !readToken().isBlank() || !readApiKey().isBlank();
    }

    String similarMovies(long tmdbMovieId) throws Exception {
        String url = "https://api.themoviedb.org/3/movie/" + tmdbMovieId
            + "/similar?language=" + URLEncoder.encode("ko-KR", StandardCharsets.UTF_8) + "&page=1";
        String apiKey = readApiKey();
        if (!apiKey.isBlank()) url += "&api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Accept", "application/json");
        String token = readToken();
        if (!token.isBlank()) request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = client.send(request.GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("TMDB API error " + response.statusCode());
        }
        return response.body();
    }

    private static String readApiKey() { return value("TMDB_API_KEY"); }
    private static String readToken() { return value("TMDB_READ_TOKEN"); }
    private static String value(String name) {
        String system = System.getProperty(name);
        if (system != null && !system.isBlank()) return system.trim();
        String environment = System.getenv(name);
        return environment == null ? "" : environment.trim();
    }
}
