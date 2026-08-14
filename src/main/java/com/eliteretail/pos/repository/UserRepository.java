package com.eliteretail.pos.repository;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.User;
import com.eliteretail.pos.model.UserRole;
import com.eliteretail.pos.utils.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserRepository {
    private static final Logger LOGGER = Logger.getLogger(UserRepository.class.getName());
    private static final String LOGIN_SQL =
            "SELECT cashier_name, cashier_code, password, role FROM users WHERE cashier_code = ?";

    public User authenticate(String cashierCode, String password) {
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {

            statement.setString(1, cashierCode);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !PasswordUtils.verify(password, resultSet.getString("password"))) {
                    return null;
                }

                UserRole role = parseRole(resultSet.getString("role"));
                return new User(
                        resultSet.getString("cashier_name"),
                        resultSet.getString("cashier_code"),
                        role
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Authentication query failed.", e);
            return null;
        }
    }

    private UserRole parseRole(String roleValue) {
        if (roleValue == null) {
            return UserRole.CASHIER;
        }
        try {
            return UserRole.valueOf(roleValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            LOGGER.warning(() -> "Unknown user role '" + roleValue + "'; defaulting to CASHIER.");
            return UserRole.CASHIER;
        }
    }
}
