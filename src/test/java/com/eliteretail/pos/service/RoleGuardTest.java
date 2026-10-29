package com.eliteretail.pos.service;

import com.eliteretail.pos.model.User;
import com.eliteretail.pos.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleGuardTest {
    private final User manager = new User("Demo Manager", "manager001", UserRole.MANAGER);
    private final User cashier = new User("Demo Cashier", "cashier001", UserRole.CASHIER);

    @Test
    void unauthenticatedUsersCannotAccessOperations() {
        assertThrows(SecurityException.class, () -> RoleGuard.requireSignedIn(null));
        assertThrows(SecurityException.class, () -> RoleGuard.requireManager(null));
        assertThrows(SecurityException.class, () -> RoleGuard.requireCashier(null));
    }

    @Test
    void managerCanManageButNotProcessCashierSales() {
        assertDoesNotThrow(() -> RoleGuard.requireSignedIn(manager));
        assertDoesNotThrow(() -> RoleGuard.requireManager(manager));
        assertThrows(SecurityException.class, () -> RoleGuard.requireCashier(manager));
    }

    @Test
    void cashierCanSellButCannotManageProductsOrDiscounts() {
        assertDoesNotThrow(() -> RoleGuard.requireSignedIn(cashier));
        assertDoesNotThrow(() -> RoleGuard.requireCashier(cashier));
        assertThrows(SecurityException.class, () -> RoleGuard.requireManager(cashier));
    }
}
