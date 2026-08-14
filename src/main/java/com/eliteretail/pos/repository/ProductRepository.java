package com.eliteretail.pos.repository;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.ProductItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProductRepository {
    private static final Logger LOGGER = Logger.getLogger(ProductRepository.class.getName());
    private static final String SELECT_COLUMNS =
            "code, name, description, unit_price, stock_quantity, category";

    public List<ProductItem> findAll() {
        return findAll(false);
    }

    public List<ProductItem> findAvailable() {
        return findAll(true);
    }

    private List<ProductItem> findAll(boolean inStockOnly) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM product" +
                (inStockOnly ? " WHERE stock_quantity > 0" : "") + " ORDER BY name";
        List<ProductItem> products = new ArrayList<>();

        try (Connection connection = DatabaseConnector.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                products.add(mapProduct(resultSet));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load products.", e);
        }
        return products;
    }

    public ProductItem findByCode(String code) {
        String sql = "SELECT " + SELECT_COLUMNS + " FROM product WHERE code = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? mapProduct(resultSet) : null;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to find product by code.", e);
            return null;
        }
    }

    public boolean insert(ProductItem product) {
        String sql = "INSERT INTO product (code, name, description, unit_price, stock_quantity, category) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getCode());
            statement.setString(2, product.getName());
            statement.setString(3, product.getDescription());
            statement.setDouble(4, product.getUnitPrice());
            statement.setInt(5, product.getStockQuantity());
            statement.setString(6, product.getCategory());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to insert product.", e);
            return false;
        }
    }

    public boolean update(ProductItem product) {
        String sql = "UPDATE product SET name = ?, description = ?, unit_price = ?, stock_quantity = ?, category = ? " +
                "WHERE code = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName());
            statement.setString(2, product.getDescription());
            statement.setDouble(3, product.getUnitPrice());
            statement.setInt(4, product.getStockQuantity());
            statement.setString(5, product.getCategory());
            statement.setString(6, product.getCode());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to update product.", e);
            return false;
        }
    }

    public boolean delete(String code) {
        String sql = "DELETE FROM product WHERE code = ?";
        try (Connection connection = DatabaseConnector.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to delete product.", e);
            return false;
        }
    }

    public void decrementStock(Connection connection, List<ProductItem> items) throws SQLException {
        String sql = "UPDATE product SET stock_quantity = stock_quantity - ? " +
                "WHERE code = ? AND stock_quantity >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (ProductItem item : items) {
                int quantity = item.getStockQuantity();
                statement.setInt(1, quantity);
                statement.setString(2, item.getCode());
                statement.setInt(3, quantity);
                statement.addBatch();
            }
            int[] updates = statement.executeBatch();
            for (int update : updates) {
                if (update == 0) {
                    throw new SQLException("Insufficient stock detected while completing the sale.");
                }
            }
        }
    }

    private ProductItem mapProduct(ResultSet resultSet) throws SQLException {
        return new ProductItem(
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("description"),
                resultSet.getDouble("unit_price"),
                resultSet.getInt("stock_quantity"),
                resultSet.getString("category")
        );
    }
}
