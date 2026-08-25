package community;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** MySQL JDBC connection factory. Set DB_URL, DB_USER and DB_PASSWORD before starting. */
public final class Database {
    private Database() { }

    public static Connection connect() throws SQLException {
        String url = value("DB_URL", "jdbc:mysql://localhost:3306/movie_community_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul");
        String user = value("DB_USER", "root");
        String password = value("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String value(String name, String fallback) {
        String system = System.getProperty(name);
        if (system != null && !system.isBlank()) return system;
        String environment = System.getenv(name);
        return environment == null || environment.isBlank() ? fallback : environment;
    }
}
