package com.eliteretail.pos.database;

import com.eliteretail.pos.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnector {
    private DatabaseConnector() {}

    public static Connection getConnection() throws SQLException {
        try {
            DatabaseConfig config = DatabaseConfig.load();
            return DriverManager.getConnection(config.getUrl(), config.getUsername(), config.getPassword());
        } catch (IllegalStateException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
