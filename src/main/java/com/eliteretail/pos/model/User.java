package com.eliteretail.pos.model;

import java.util.Objects;

public final class User {
    private final String cashierName;
    private final String cashierCode;
    private final UserRole role;

    public User(String cashierName, String cashierCode, UserRole role) {
        this.cashierName = Objects.requireNonNullElse(cashierName, "");
        this.cashierCode = Objects.requireNonNull(cashierCode, "cashierCode");
        this.role = Objects.requireNonNull(role, "role");
    }

    public String getCashierName() { return cashierName; }
    public String getCashierCode() { return cashierCode; }
    public UserRole getRole() { return role; }
    public boolean isManager() { return role == UserRole.MANAGER; }
    public boolean isCashier() { return role == UserRole.CASHIER; }

    @Override
    public String toString() {
        return "User{cashierName='" + cashierName + "', cashierCode='" + cashierCode + "', role=" + role + "}";
    }
}
