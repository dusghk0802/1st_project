package project.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Reads OS environment variables first, then falls back to the .env file in the working directory. */
public final class Env {
    private static final Map<String, String> DOT_ENV = load(Path.of(".env"));

    private Env() { }

    public static String get(String key) {
        String environment = System.getenv(key);
        if (environment != null && !environment.isBlank()) return environment;
        return DOT_ENV.get(key);
    }

    private static Map<String, String> load(Path file) {
        Map<String, String> values = new HashMap<>();
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = raw.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).strip();
                String value = line.substring(eq + 1).strip();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"") || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                values.put(key, value);
            }
        } catch (IOException error) {
            System.err.println("Failed to read .env: " + error.getMessage());
        }
        return values;
    }
}
