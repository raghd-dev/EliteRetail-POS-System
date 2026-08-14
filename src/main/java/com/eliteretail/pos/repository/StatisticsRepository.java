package com.eliteretail.pos.repository;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.ProductStats;
import com.eliteretail.pos.model.RecentTransaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatisticsRepository {
    public double getTotalSalesToday() throws SQLException {
        return queryDouble("SELECT COALESCE(SUM(total), 0) FROM bills WHERE DATE(created_at) = CURDATE()");
    }

    public int getTotalTransactionsToday() throws SQLException {
        return (int) queryDouble("SELECT COUNT(*) FROM bills WHERE DATE(created_at) = CURDATE()");
    }

    public double getAverageTransactionValue() throws SQLException {
        return queryDouble("SELECT COALESCE(AVG(total), 0) FROM bills WHERE DATE(created_at) = CURDATE()");
    }

    public double getTotalDiscountsToday() throws SQLException {
        return queryDouble("SELECT COALESCE(SUM(total_discount), 0) FROM bills WHERE DATE(created_at) = CURDATE()");
    }

    public Map<String, Double> getSalesByHour() throws SQLException {
        String sql = "SELECT DATE_FORMAT(created_at, '%H:00') AS sale_hour, COALESCE(SUM(total), 0) AS sales " +
                "FROM bills WHERE DATE(created_at) = CURDATE() GROUP BY HOUR(created_at) ORDER BY HOUR(created_at)";
        Map<String, Double> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnector.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString("sale_hour"), rs.getDouble("sales"));
            }
        }
        return result;
    }

    public Map<String, Double> getTopSellingProducts(int limit) throws SQLException {
        Map<String, ProductStats> detailed = getTopSellingProductsDetailed(limit);
        Map<String, Double> result = new LinkedHashMap<>();
        detailed.forEach((name, stats) -> result.put(name, stats.salesAmount()));
        return result;
    }

    public Map<String, ProductStats> getTopSellingProductsDetailed(int limit) throws SQLException {
        String sql = "SELECT bi.product_name, " +
                "SUM((bi.unit_price - bi.discount) * bi.quantity) AS sales_amount, " +
                "SUM(bi.quantity) AS quantity_sold " +
                "FROM bill_items bi JOIN bills b ON b.id = bi.bill_id " +
                "WHERE DATE(b.created_at) = CURDATE() " +
                "GROUP BY bi.product_name ORDER BY quantity_sold DESC, sales_amount DESC LIMIT ?";
        Map<String, ProductStats> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("product_name"),
                            new ProductStats(rs.getDouble("sales_amount"), rs.getInt("quantity_sold")));
                }
            }
        }
        return result;
    }


    public List<RecentTransaction> getRecentTransactions(int limit) throws SQLException {
        String sql = "SELECT bill_number, total, payment_method, created_at FROM bills " +
                "ORDER BY created_at DESC LIMIT ?";
        List<RecentTransaction> result = new ArrayList<>();
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, Math.max(1, limit));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    var timestamp = rs.getTimestamp("created_at");
                    result.add(new RecentTransaction(
                            rs.getString("bill_number"),
                            rs.getDouble("total"),
                            rs.getString("payment_method"),
                            timestamp == null ? null : timestamp.toLocalDateTime()
                    ));
                }
            }
        }
        return result;
    }

    public Map<String, Integer> getPaymentMethodStats() throws SQLException {
        String sql = "SELECT COALESCE(payment_method, 'UNKNOWN') AS method, COUNT(*) AS method_count " +
                "FROM bills WHERE DATE(created_at) = CURDATE() GROUP BY payment_method ORDER BY method_count DESC";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnector.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                result.put(rs.getString("method"), rs.getInt("method_count"));
            }
        }
        return result;
    }

    private double queryDouble(String sql) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}
