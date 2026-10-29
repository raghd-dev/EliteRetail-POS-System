package com.eliteretail.pos.repository;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

public class BillRepository {
    public int getNextDailySerial(String machineNumber) throws SQLException {
        String sql = "SELECT COALESCE(MAX(serial_number), 0) + 1 AS next_serial " +
                "FROM bills WHERE machine_number = ? AND DATE(created_at) = CURDATE()";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, machineNumber);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("next_serial") : 1;
            }
        }
    }

    /**
     * Coordinates daily serial allocation across separate application instances.
     * MySQL named locks are connection-scoped (not transaction-scoped), so the
     * caller must release after COMMIT or ROLLBACK on this same connection.
     */
    public void acquireSerialLock(Connection connection, String machineNumber) throws SQLException {
        String sql = "SELECT GET_LOCK(?, 10)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, lockName(machineNumber));
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1 || result.wasNull()) {
                    throw new SQLException("Could not reserve a receipt number. Please retry checkout.");
                }
            }
        }
    }

    public void releaseSerialLock(Connection connection, String machineNumber) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName(machineNumber));
            statement.executeQuery();
        }
    }

    private static String lockName(String machineNumber) throws SQLException {
        if (machineNumber == null || !machineNumber.matches("[a-zA-Z0-9_-]{1,32}")) {
            throw new SQLException("Invalid POS machine number.");
        }
        return "eliteretail:serial:" + machineNumber;
    }

    /** Called only while holding acquireSerialLock, on the sale connection. */
    public BillNumber allocateBillNumber(Connection connection, String machineNumber) throws SQLException {
        String sql = "SELECT COALESCE(MAX(serial_number), 0) + 1 " +
                "FROM bills WHERE machine_number = ? AND DATE(created_at) = CURDATE()";
        int serial;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, machineNumber);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Unable to allocate daily serial.");
                serial = result.getInt(1);
            }
        }
        String date;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT DATE_FORMAT(CURDATE(), '%Y%m%d')")) {
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw new SQLException("Unable to read the database date.");
                date = result.getString(1);
            }
        }
        return new BillNumber(String.format(Locale.ROOT, "%s-%s-%04d", machineNumber, date, serial), serial);
    }

    public record BillNumber(String value, int serial) {}

    public int insertBill(
            Connection connection,
            String cashierCode,
            String cashierName,
            String billNumber,
            String machineNumber,
            int serialNumber,
            double totalAmount,
            double totalDiscount,
            PaymentMethod paymentMethod,
            double amountReceived,
            double changeAmount
    ) throws SQLException {
        String sql = "INSERT INTO bills (bill_number, machine_number, serial_number, cashier_code, cashier_name, " +
                "total, total_discount, payment_method, amount_received, change_amount, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, billNumber);
            statement.setString(2, machineNumber);
            statement.setInt(3, serialNumber);
            statement.setString(4, cashierCode);
            statement.setString(5, cashierName);
            statement.setDouble(6, totalAmount);
            statement.setDouble(7, totalDiscount);
            statement.setString(8, paymentMethod.name());
            statement.setDouble(9, amountReceived);
            statement.setDouble(10, changeAmount);

            if (statement.executeUpdate() == 0) {
                throw new SQLException("Creating bill failed; no rows were affected.");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating bill failed; no generated id was returned.");
                }
                return keys.getInt(1);
            }
        }
    }

    public void insertItems(Connection connection, int billId, List<ProductItem> items) throws SQLException {
        String sql = "INSERT INTO bill_items " +
                "(bill_id, product_code, product_name, unit_price, quantity, discount) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ProductItem item : items) {
                statement.setInt(1, billId);
                statement.setString(2, item.getCode());
                statement.setString(3, item.getName());
                statement.setDouble(4, item.getUnitPrice());
                statement.setInt(5, item.getStockQuantity());
                statement.setDouble(6, item.getDiscount());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
