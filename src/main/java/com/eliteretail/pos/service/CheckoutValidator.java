package com.eliteretail.pos.service;

import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lightweight preflight validation; the database still enforces stock and uniqueness. */
public final class CheckoutValidator {
    private CheckoutValidator() {}

    public static void validate(double total, double totalDiscount, PaymentMethod method,
                                double received, double change, List<ProductItem> items)
            throws SQLException {
        if (method == null || items == null || items.isEmpty()) {
            throw new SQLException("Payment method and at least one item are required.");
        }
        BigDecimal finalTotal = cents(total, "total");
        BigDecimal discount = cents(totalDiscount, "discount");
        BigDecimal amountReceived = cents(received, "amount received");
        BigDecimal changeAmount = cents(change, "change");
        if (finalTotal.signum() < 0 || discount.signum() < 0 ||
                amountReceived.signum() < 0 || changeAmount.signum() < 0) {
            throw new SQLException("Sale and payment amounts must not be negative.");
        }
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal itemDiscount = BigDecimal.ZERO;
        Set<String> productCodes = new HashSet<>();
        for (ProductItem item : items) {
            if (item == null || item.getCode() == null || item.getCode().isBlank() ||
                    !productCodes.add(item.getCode()) || item.getStockQuantity() <= 0) {
                throw new SQLException("Each cart line must have a unique product and positive quantity.");
            }
            BigDecimal unitPrice = cents(item.getUnitPrice(), "unit price");
            BigDecimal unitDiscount = checked(item.getDiscount(), "unit discount");
            if (unitPrice.signum() < 0 || unitDiscount.signum() < 0 ||
                    unitDiscount.compareTo(unitPrice) > 0) {
                throw new SQLException("Invalid product price or discount.");
            }
            BigDecimal quantity = BigDecimal.valueOf(item.getStockQuantity());
            gross = gross.add(unitPrice.multiply(quantity));
            itemDiscount = itemDiscount.add(unitDiscount.multiply(quantity));
        }
        if (discount.compareTo(itemDiscount.setScale(2, RoundingMode.HALF_UP)) != 0 ||
                finalTotal.compareTo(gross.subtract(itemDiscount).setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new SQLException("The cart amounts changed. Refresh the cart before checkout.");
        }
        if (method == PaymentMethod.CASH) {
            if (amountReceived.compareTo(finalTotal) < 0 ||
                    changeAmount.compareTo(amountReceived.subtract(finalTotal)) != 0) {
                throw new SQLException("Insufficient cash or invalid change amount.");
            }
        } else if (amountReceived.compareTo(finalTotal) != 0 || changeAmount.signum() != 0) {
            throw new SQLException("Non-cash payment must match the sale total with no change.");
        }
    }

    private static BigDecimal checked(double number, String field) throws SQLException {
        if (!Double.isFinite(number)) {
            throw new SQLException("Invalid " + field + ": amount must be finite.");
        }
        return BigDecimal.valueOf(number);
    }

    private static BigDecimal cents(double number, String field) throws SQLException {
        return checked(number, field).setScale(2, RoundingMode.HALF_UP);
    }
}
