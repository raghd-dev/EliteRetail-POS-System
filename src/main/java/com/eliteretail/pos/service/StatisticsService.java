package com.eliteretail.pos.service;

import com.eliteretail.pos.model.ProductStats;
import com.eliteretail.pos.model.RecentTransaction;
import com.eliteretail.pos.repository.StatisticsRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StatisticsService {
    private static final Logger LOGGER = Logger.getLogger(StatisticsService.class.getName());
    private final StatisticsRepository repository = new StatisticsRepository();

    public double getTotalSalesToday() { return safely(repository::getTotalSalesToday, 0.0); }
    public int getTotalTransactionsToday() { return safely(repository::getTotalTransactionsToday, 0); }
    public double getAverageTransactionValue() { return safely(repository::getAverageTransactionValue, 0.0); }
    public double getTotalDiscountsToday() { return safely(repository::getTotalDiscountsToday, 0.0); }
    public Map<String, Double> getSalesByHour() { return safely(repository::getSalesByHour, Map.of()); }
    public Map<String, Double> getTopSellingProducts(int limit) {
        return safely(() -> repository.getTopSellingProducts(limit), Map.of());
    }
    public Map<String, ProductStats> getTopSellingProductsDetailed(int limit) {
        return safely(() -> repository.getTopSellingProductsDetailed(limit), Map.of());
    }
    public Map<String, Integer> getPaymentMethodStats() {
        return safely(repository::getPaymentMethodStats, Map.of());
    }
    public List<RecentTransaction> getRecentTransactions(int limit) {
        return safely(() -> repository.getRecentTransactions(limit), List.of());
    }

    private <T> T safely(SqlSupplier<T> supplier, T fallback) {
        RoleGuard.requireManager(AuthService.getInstance().getCurrentUser());
        try {
            return supplier.get();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load statistics.", e);
            return fallback;
        }
    }

    @FunctionalInterface
    private interface SqlSupplier<T> {
        T get() throws SQLException;
    }
}
