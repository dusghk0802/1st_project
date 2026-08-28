package project;

import project.common.DBConnection;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Imports only genres with no linked movies. Each selected genre receives up to 100 TMDB movies. */
public final class TmdbUnfilledGenreImporter {
    private static final int TARGET_PER_GENRE = 100;
    private static final int MAX_PAGES = 12;
    private final HttpClient http = HttpClient.newHttpClient();
    private final String apiKey = value("TMDB_API_KEY");
    private final String readToken = value("TMDB_READ_TOKEN");

    public static void main(String[] args) throws Exception {
        int target = args.length > 0 ? Integer.parseInt(args[0]) : TARGET_PER_GENRE;
        boolean dryRun = args.length > 1 && "--dry-run".equals(args[1]);
        boolean resume = args.length > 1 && "--resume".equals(args[1]);
        TmdbUnfilledGenreImporter importer = new TmdbUnfilledGenreImporter();
        importer.requireKey();
        importer.upsertGenres();
        List<Genre> genres = importer.targetGenres(target, resume);
        System.out.println("unfilledGenres=" + genres.size() + ", targetPerGenre=" + target);
        for (Genre genre : genres) {
            System.out.println("genre=" + genre.id + " (" + genre.name + "), current=0");
            if (!dryRun) importer.fillGenre(genre, target);
        }
    }

    private void requireKey() { if (apiKey.isBlank() && readToken.isBlank()) throw new IllegalStateException("TMDB_API_KEY or TMDB_READ_TOKEN is not configured."); }

    private void upsertGenres() throws Exception {
        Map<String,Object> root = object(json("/genre/movie/list?language=ko-KR"));
        String merge = "MERGE INTO genres g USING (SELECT ? genre_id, ? name FROM dual) v ON (g.genre_id=v.genre_id) WHEN MATCHED THEN UPDATE SET g.name=v.name WHEN NOT MATCHED THEN INSERT (genre_id,name) VALUES (v.genre_id,v.name)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(merge)) {
            for (Object row : list(root.get("genres"))) { Map<String,Object> genre = object(row); s.setInt(1, number(genre.get("id")).intValue()); s.setString(2, string(genre.get("name"))); s.addBatch(); }
            s.executeBatch();
        }
    }

    private List<Genre> targetGenres(int target, boolean resume) throws Exception {
        String sql = "SELECT g.genre_id,g.name FROM genres g LEFT JOIN movie_genres mg ON mg.genre_id=g.genre_id GROUP BY g.genre_id,g.name HAVING COUNT(mg.movie_id)" + (resume ? " < ?" : "=0") + " ORDER BY g.genre_id";
        List<Genre> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) { if (resume) s.setInt(1, target); try (ResultSet r = s.executeQuery()) { while (r.next()) result.add(new Genre(r.getInt(1), r.getString(2))); } }
        return result;
    }

    private void fillGenre(Genre genre, int target) throws Exception {
        for (int page = 1; page <= MAX_PAGES && count(genre.id) < target; page++) {
            Map<String,Object> root = object(json("/discover/movie?include_adult=false&include_video=false&language=ko-KR&sort_by=popularity.desc&with_genres=" + genre.id + "&page=" + page));
            List<Object> movies = list(root.get("results"));
            if (movies.isEmpty()) break;
            for (Object row : movies) { if (count(genre.id) >= target) break; try { saveMovie(object(row), genre.id); } catch (Exception error) { System.err.println("  skipped TMDB movie: " + error.getMessage()); } }
            System.out.println("  page=" + page + ", imported=" + count(genre.id));
        }
        System.out.println("completed genre=" + genre.name + ", total=" + count(genre.id));
    }

    private void saveMovie(Map<String,Object> movie, int genreId) throws Exception {
        long tmdbId = number(movie.get("id")).longValue();
        String title = string(movie.get("title"));
        if (tmdbId <= 0 || title.isBlank()) return;
        String merge = "MERGE INTO movies m USING (SELECT ? tmdb_id FROM dual) v ON (m.tmdb_id=v.tmdb_id) "
                + "WHEN MATCHED THEN UPDATE SET title=?,original_title=?,overview=?,release_date=?,poster_path=?,backdrop_path=?,vote_average=?,vote_count=?,popularity=?,updated_at=CURRENT_TIMESTAMP "
                + "WHEN NOT MATCHED THEN INSERT (tmdb_id,title,original_title,overview,release_date,poster_path,backdrop_path,vote_average,vote_count,popularity,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)";
        try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement(merge)) {
            s.setLong(1, tmdbId); bindMovie(s, 2, movie, tmdbId, false); bindMovie(s, 11, movie, tmdbId, true); s.executeUpdate();
            long movieId;
            try (PreparedStatement find = c.prepareStatement("SELECT movie_id FROM movies WHERE tmdb_id=?")) { find.setLong(1, tmdbId); try (ResultSet r = find.executeQuery()) { r.next(); movieId = r.getLong(1); } }
            try (PreparedStatement link = c.prepareStatement("MERGE INTO movie_genres mg USING (SELECT ? movie_id, ? genre_id FROM dual) v ON (mg.movie_id=v.movie_id AND mg.genre_id=v.genre_id) WHEN NOT MATCHED THEN INSERT (movie_id,genre_id) VALUES (v.movie_id,v.genre_id)")) { link.setLong(1, movieId); link.setInt(2, genreId); link.executeUpdate(); }
        }
    }

    private void bindMovie(PreparedStatement s, int offset, Map<String,Object> m, long tmdbId, boolean insert) throws Exception {
        int i = offset;
        if (insert) s.setLong(i++, tmdbId);
        s.setString(i++, string(m.get("title"))); s.setString(i++, string(m.get("original_title"))); s.setString(i++, string(m.get("overview")));
        String rawDate = string(m.get("release_date")); if (rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) s.setDate(i++, Date.valueOf(LocalDate.parse(rawDate))); else s.setNull(i++, Types.DATE);
        s.setString(i++, string(m.get("poster_path"))); s.setString(i++, string(m.get("backdrop_path"))); s.setBigDecimal(i++, BigDecimal.valueOf(number(m.get("vote_average")).doubleValue())); s.setLong(i++, number(m.get("vote_count")).longValue()); s.setBigDecimal(i++, BigDecimal.valueOf(number(m.get("popularity")).doubleValue()));
    }

    private int count(int genreId) throws Exception { try (Connection c = DBConnection.getConnection(); PreparedStatement s = c.prepareStatement("SELECT COUNT(*) FROM movie_genres WHERE genre_id=?")) { s.setInt(1, genreId); try (ResultSet r = s.executeQuery()) { r.next(); return r.getInt(1); } } }
    private Object json(String path) throws Exception { String separator = path.contains("?") ? "&" : "?"; String url = "https://api.themoviedb.org/3" + path + (!apiKey.isBlank() ? separator + "api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8) : ""); HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json").GET(); if (!readToken.isBlank()) b.header("Authorization", "Bearer " + readToken); HttpResponse<String> response = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); if (response.statusCode() / 100 != 2) throw new IllegalStateException("TMDB HTTP " + response.statusCode()); return SimpleJson.parse(response.body()); }
    private static String value(String key) { String property = System.getProperty(key); if (property != null && !property.isBlank()) return property.trim(); String environment = System.getenv(key); return environment == null ? "" : environment.trim(); }
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value) { return value instanceof Map ? (Map<String,Object>) value : Collections.emptyMap(); }
    @SuppressWarnings("unchecked") private static List<Object> list(Object value) { return value instanceof List ? (List<Object>) value : Collections.emptyList(); }
    private static String string(Object value) { return value == null ? "" : String.valueOf(value); }
    private static Number number(Object value) { return value instanceof Number ? (Number) value : Long.valueOf(0); }
    private static final class Genre { final int id; final String name; Genre(int id, String name) { this.id = id; this.name = name; } }
}
