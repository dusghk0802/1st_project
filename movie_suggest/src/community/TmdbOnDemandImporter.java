package community;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Saves the first TMDB title match when the local catalog has no keyword match. */
final class TmdbOnDemandImporter {
    long importFirst(String query) throws Exception {
        TmdbClient tmdb = new TmdbClient();
        Map<String,Object> root = object(SimpleJson.parse(tmdb.searchMovies(query, 1)));
        List<Object> results = list(root.get("results")); if (results.isEmpty()) return 0;
        Map<String,Object> movie = object(results.get(0)); long tmdbId = number(movie.get("id")).longValue();
        Map<String,Object> english = object(SimpleJson.parse(tmdb.movieDetails(tmdbId, "en-US")));
        String title = string(movie.get("title")); if (title.isBlank()) return 0;
        try (Connection connection = Database.connect()) {
            String sql="INSERT INTO movies (tmdb_id,title,original_title,title_en,overview,overview_en,release_date,poster_path,backdrop_path,vote_average,vote_count,popularity) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title),original_title=VALUES(original_title),title_en=VALUES(title_en),overview=VALUES(overview),overview_en=VALUES(overview_en),poster_path=VALUES(poster_path),backdrop_path=VALUES(backdrop_path),popularity=VALUES(popularity)";
            try(PreparedStatement s=connection.prepareStatement(sql)){
                s.setLong(1,tmdbId);s.setString(2,title);s.setString(3,string(movie.get("original_title")));s.setString(4,string(english.get("title")));s.setString(5,string(movie.get("overview")));s.setString(6,string(english.get("overview")));
                String date=string(movie.get("release_date"));if(date.matches("\\d{4}-\\d{2}-\\d{2}"))s.setString(7,date);else s.setNull(7,java.sql.Types.DATE);s.setString(8,string(movie.get("poster_path")));s.setString(9,string(movie.get("backdrop_path")));s.setBigDecimal(10,BigDecimal.valueOf(number(movie.get("vote_average")).doubleValue()));s.setInt(11,number(movie.get("vote_count")).intValue());s.setBigDecimal(12,BigDecimal.valueOf(number(movie.get("popularity")).doubleValue()));s.executeUpdate();
            }
            long movieId;try(PreparedStatement s=connection.prepareStatement("SELECT movie_id FROM movies WHERE tmdb_id=?")){s.setLong(1,tmdbId);try(ResultSet r=s.executeQuery()){r.next();movieId=r.getLong(1);}}
            savePoster(connection,movieId,tmdbId,string(movie.get("poster_path")));return movieId;
        }
    }
    /** Imports a known TMDB movie id; used to cache TMDB's official related titles locally. */
    long importTmdbId(long tmdbId) throws Exception {
        TmdbClient tmdb = new TmdbClient();
        Map<String,Object> movie = object(SimpleJson.parse(tmdb.movieDetails(tmdbId, "ko-KR")));
        Map<String,Object> english = object(SimpleJson.parse(tmdb.movieDetails(tmdbId, "en-US")));
        String title = string(movie.get("title")); if (title.isBlank()) return 0;
        try (Connection connection = Database.connect()) {
            String sql="INSERT INTO movies (tmdb_id,title,original_title,title_en,overview,overview_en,release_date,poster_path,backdrop_path,vote_average,vote_count,popularity) VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE title=VALUES(title),original_title=VALUES(original_title),title_en=VALUES(title_en),overview=VALUES(overview),overview_en=VALUES(overview_en),poster_path=VALUES(poster_path),backdrop_path=VALUES(backdrop_path),popularity=VALUES(popularity)";
            try(PreparedStatement s=connection.prepareStatement(sql)){
                s.setLong(1,tmdbId);s.setString(2,title);s.setString(3,string(movie.get("original_title")));s.setString(4,string(english.get("title")));s.setString(5,string(movie.get("overview")));s.setString(6,string(english.get("overview")));
                String date=string(movie.get("release_date"));if(date.matches("\\d{4}-\\d{2}-\\d{2}"))s.setString(7,date);else s.setNull(7,java.sql.Types.DATE);s.setString(8,string(movie.get("poster_path")));s.setString(9,string(movie.get("backdrop_path")));s.setBigDecimal(10,BigDecimal.valueOf(number(movie.get("vote_average")).doubleValue()));s.setInt(11,number(movie.get("vote_count")).intValue());s.setBigDecimal(12,BigDecimal.valueOf(number(movie.get("popularity")).doubleValue()));s.executeUpdate();
            }
            long movieId;try(PreparedStatement s=connection.prepareStatement("SELECT movie_id FROM movies WHERE tmdb_id=?")){s.setLong(1,tmdbId);try(ResultSet r=s.executeQuery()){r.next();movieId=r.getLong(1);}}
            savePoster(connection,movieId,tmdbId,string(movie.get("poster_path")));return movieId;
        }
    }
    private void savePoster(Connection c,long id,long tmdbId,String posterPath)throws Exception{String url=TmdbClient.posterUrl(posterPath);if(url==null)return;Path target=Path.of("assets","posters",tmdbId+".jpg");Files.createDirectories(target.getParent());if(!Files.exists(target)){HttpResponse<byte[]>r=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(),HttpResponse.BodyHandlers.ofByteArray());if(r.statusCode()/100!=2)return;Files.write(target,r.body());}try(PreparedStatement s=c.prepareStatement("UPDATE movies SET poster_local_path=? WHERE movie_id=?")){s.setString(1,target.toString().replace('\\','/'));s.setLong(2,id);s.executeUpdate();}}
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object v){return v instanceof Map?(Map<String,Object>)v:Collections.emptyMap();}
    @SuppressWarnings("unchecked") private static List<Object> list(Object v){return v instanceof List?(List<Object>)v:Collections.emptyList();}
    private static String string(Object v){return v==null?"":String.valueOf(v);} private static Number number(Object v){return v instanceof Number?(Number)v:Integer.valueOf(0);}
}
