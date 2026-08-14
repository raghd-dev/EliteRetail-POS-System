package com.eliteretail.pos.repository;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.Discount;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscountRepository {
    private static final Logger LOGGER = Logger.getLogger(DiscountRepository.class.getName());
    public List<String> getAllProductNames() {
        List<String> productNames = new ArrayList<>();
        String sql = "SELECT name FROM product ORDER BY name";

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                productNames.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load product names.", e);
        }
        return productNames;
    }

    public List<Discount> getAllDiscounts() {
        List<Discount> discounts = new ArrayList<>();
        String sql = "SELECT * FROM discounts ORDER BY active DESC, start_date DESC";

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Discount discount = createDiscountFromResultSet(rs);
                discounts.add(discount);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load discounts.", e);
        }
        return discounts;
    }

    public Discount getDiscountById(int id) {
        String sql = "SELECT * FROM discounts WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return createDiscountFromResultSet(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to find discount by id.", e);
        }
        return null;
    }



    public boolean addDiscount(Discount discount) {
        String sql = "INSERT INTO discounts (name, description, value, type, apply_to, target_value, " +
                "start_date, end_date, min_quantity, min_purchase_amount, active, usage_limit, times_used) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setDiscountParameters(stmt, discount);
            stmt.setInt(12, discount.getUsageLimit());
            stmt.setInt(13, discount.getTimesUsed());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    discount.setId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to add discount.", e);
        }
        return false;
    }

    public boolean updateDiscount(Discount discount) {
        String sql = "UPDATE discounts SET name=?, description=?, value=?, type=?, apply_to=?, " +
                "target_value=?, start_date=?, end_date=?, min_quantity=?, min_purchase_amount=?, " +
                "active=?, usage_limit=?, times_used=? WHERE id=?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setDiscountParameters(stmt, discount);
            stmt.setInt(12, discount.getUsageLimit());
            stmt.setInt(13, discount.getTimesUsed());
            stmt.setInt(14, discount.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to update discount.", e);
        }
        return false;
    }

    public boolean deleteDiscount(int id) {
        String sql = "DELETE FROM discounts WHERE id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to delete discount.", e);
        }
        return false;
    }

    public List<Discount> getActiveDiscounts() {
        List<Discount> discounts = new ArrayList<>();
        String sql = "SELECT * FROM discounts WHERE active = true AND start_date <= CURDATE() " +
                "AND end_date >= CURDATE() AND (usage_limit = 0 OR times_used < usage_limit)";

        try (Connection conn = DatabaseConnector.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                discounts.add(createDiscountFromResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load active discounts.", e);
        }
        return discounts;
    }

    private Discount createDiscountFromResultSet(ResultSet rs) throws SQLException {
        return new Discount(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("value"),
                rs.getString("type"),
                rs.getString("apply_to"),
                rs.getString("target_value"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                rs.getInt("min_quantity"),
                rs.getDouble("min_purchase_amount"),
                rs.getBoolean("active"),
                rs.getInt("usage_limit"),
                rs.getInt("times_used")
        );
    }

    private void setDiscountParameters(PreparedStatement stmt, Discount discount) throws SQLException {
        stmt.setString(1, discount.getName());
        stmt.setString(2, discount.getDescription());
        stmt.setDouble(3, discount.getValue());
        stmt.setString(4, discount.getType());
        stmt.setString(5, discount.getApplyTo());
        stmt.setString(6, discount.getTargetValue());
        stmt.setDate(7, Date.valueOf(discount.getStartDate()));
        stmt.setDate(8, Date.valueOf(discount.getEndDate()));
        stmt.setInt(9, discount.getMinQuantity());
        stmt.setDouble(10, discount.getMinPurchaseAmount());
        stmt.setBoolean(11, discount.isActive());
    }
}