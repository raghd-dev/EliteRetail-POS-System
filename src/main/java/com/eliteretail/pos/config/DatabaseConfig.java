package com.eliteretail.pos.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DatabaseConfig {
    private static final Path CONFIG_FILE = Path.of("database.properties");

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConfig(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConfig load() {
        Properties properties = new Properties();
        if (Files.isRegularFile(CONFIG_FILE)) {
            try (InputStream input = Files.newInputStream(CONFIG_FILE)) {
                properties.load(input);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read database.properties", e);
            }
        }

        String url = firstNonBlank(System.getenv("DB_URL"), properties.getProperty("db.url"));
        String username = firstNonBlank(System.getenv("DB_USERNAME"), properties.getProperty("db.username"));
        String password = firstNonBlank(System.getenv("DB_PASSWORD"), properties.getProperty("db.password"));

        if (url == null || username == null || password == null || "YOUR_PASSWORD".equals(password)) {
            throw new IllegalStateException(
                    "Database connection settings are not configured. Copy database.properties.example to " +
                    "database.properties and set db.url/db.username/db.password, or provide DB_URL, " +
                    "DB_USERNAME, and DB_PASSWORD environment variables."
            );
        }

        return new DatabaseConfig(url, username, password);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public String getUrl() { return url; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
