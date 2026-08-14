package com.eliteretail.pos.service;

import com.eliteretail.pos.model.User;
import com.eliteretail.pos.repository.UserRepository;

public final class AuthService {
    private static final AuthService INSTANCE = new AuthService();

    private final UserRepository userRepository = new UserRepository();
    private User currentUser;

    private AuthService() {}

    public static AuthService getInstance() { return INSTANCE; }

    public boolean login(String cashierCode, String password) {
        User user = userRepository.authenticate(cashierCode, password);
        if (user == null) {
            return false;
        }
        currentUser = user;
        return true;
    }

    public User getCurrentUser() { return currentUser; }
    public void logout() { currentUser = null; }
    public boolean isManager() { return currentUser != null && currentUser.isManager(); }
    public boolean isCashier() { return currentUser != null && currentUser.isCashier(); }
    public boolean canManageUsers() { return isManager(); }
    public boolean canManageProducts() { return isManager(); }
    public boolean canManageDiscounts() { return isManager(); }
    public boolean canViewReports() { return isManager(); }
    public boolean canProcessSales() { return isCashier(); }
    public boolean canViewProducts() { return currentUser != null; }
    public String getCurrentCashierName() { return currentUser == null ? "" : currentUser.getCashierName(); }
    public String getCurrentCashierCode() { return currentUser == null ? "" : currentUser.getCashierCode(); }
}
