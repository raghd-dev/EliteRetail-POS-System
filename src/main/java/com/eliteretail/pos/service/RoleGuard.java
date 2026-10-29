package com.eliteretail.pos.service;

import com.eliteretail.pos.model.User;
import com.eliteretail.pos.model.UserRole;

/**
 * Application-level access rules for this local desktop demo.
 * These checks are not database-side authorization: users with direct MySQL
 * credentials could bypass Java application services.
 */
public final class RoleGuard {
    private RoleGuard() {}

    public static void requireSignedIn(User user) {
        if (user == null) {
            throw new SecurityException("Sign in is required.");
        }
    }

    public static void requireManager(User user) {
        requireRole(user, UserRole.MANAGER);
    }

    public static void requireCashier(User user) {
        requireRole(user, UserRole.CASHIER);
    }

    private static void requireRole(User user, UserRole required) {
        requireSignedIn(user);
        if (user.getRole() != required) {
            throw new SecurityException(required + " permission is required.");
        }
    }
}
