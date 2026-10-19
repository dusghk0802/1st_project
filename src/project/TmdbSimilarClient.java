package project;
import project.common.Env;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    /** Finds a GPT-selected movie in TMDB and returns the first result with public display metadata. */
    String searchMovie(String title) throws Exception {
        String url = "https://api.themoviedb.org/3/search/movie?language="
            + URLEncoder.encode("ko-KR", StandardCharsets.UTF_8)
            + "&include_adult=false&page=1&query=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
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
        Object parsed = SimpleJson.parse(response.body());
        if (!(parsed instanceof Map)) return "";
        Object raw = ((Map<?, ?>) parsed).get("results");
        if (!(raw instanceof List)) return "";
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> movie = (Map<?, ?>) item;
            Object id = movie.get("id");
            if (!(id instanceof Number)) continue;
            String movieTitle = string(movie.get("title"));
            if (movieTitle.isBlank()) movieTitle = string(movie.get("original_title"));
            if (movieTitle.isBlank()) continue;
            long tmdbId = ((Number) id).longValue();
            String posterPath = string(movie.get("poster_path"));
            String poster = posterPath.isBlank() ? "" : "https://image.tmdb.org/t/p/w500" + posterPath;
            return "{\"id\":" + tmdbId + ",\"tmdbId\":" + tmdbId
                + ",\"title\":\"" + json(movieTitle) + "\""
                + ",\"originalTitle\":\"" + json(string(movie.get("original_title"))) + "\""
                + ",\"overview\":\"" + json(string(movie.get("overview"))) + "\""
                + ",\"poster\":\"" + json(poster) + "\""
                + ",\"voteAverage\":" + number(movie.get("vote_average"))
                + ",\"tmdbUrl\":\"https://www.themoviedb.org/movie/" + tmdbId + "\"}";
        }
        return "";
    }

    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private static String number(Object value) { return value instanceof Number ? String.valueOf(value) : "0"; }
    private static String json(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", ""); }

    private static String readApiKey() { return value("TMDB_API_KEY"); }
    private static String readToken() { return value("TMDB_READ_TOKEN"); }
    private static String value(String name) {
        String system = System.getProperty(name);
        if (system != null && !system.isBlank()) return system.trim();
        String environment = Env.get(name);
        return environment == null ? "" : environment.trim();
    }
}
