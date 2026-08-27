package project.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Oracle XE connection factory. Credentials are supplied only at runtime. */
public final class DBConnection {
    private DBConnection() { }

    public static Connection getConnection() throws SQLException {
        try { Class.forName("oracle.jdbc.OracleDriver"); }
        catch (ClassNotFoundException error) { throw new SQLException("Oracle JDBC driver (ojdbc11) is required.", error); }
        return DriverManager.getConnection(
                value("DB_URL", "jdbc:oracle:thin:@localhost:1521:XE"),
                value("DB_USER", "c##project"),
                value("DB_PASSWORD", ""));
    }

    private static String value(String key, String fallback) {
        String property = System.getProperty(key);
        if (property != null && !property.isBlank()) return property;
        String environment = System.getenv(key);
        return environment == null || environment.isBlank() ? fallback : environment;
    }
}
