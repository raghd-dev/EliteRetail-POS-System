package com.eliteretail.pos.utils;

import com.eliteretail.pos.model.ProductItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Monetary values are rounded once at the per-unit currency boundary.
 * Our MySQL schema stores unit_price and discount as DECIMAL(12,2).
 * Keep the existing JavaFX double-facing model for compatibility, but use
 * BigDecimal for arithmetic and persistence.
 */
public final class MoneyAmounts {
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyAmounts() {}

    public static BigDecimal exact(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Money must be finite.");
        }
        return BigDecimal.valueOf(value);
    }

    public static BigDecimal cents(double value) {
        return cents(exact(value));
    }

    public static BigDecimal cents(BigDecimal value) {
        if (value == null) throw new IllegalArgumentException("Money is required.");
        return value.setScale(SCALE, ROUNDING);
    }

    /** Cash input must be a nonnegative amount that can be stored in cents. */
    public static BigDecimal parseCash(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Enter an amount.");
        }
        try {
            BigDecimal amount = new BigDecimal(input.trim());
            if (amount.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative.");
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Enter a valid amount with no more than two decimal places.", e);
        }
    }

    public static BigDecimal lineGross(ProductItem item) {
        return cents(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getStockQuantity()));
    }

    public static BigDecimal lineDiscount(ProductItem item) {
        return cents(item.getDiscount()).multiply(BigDecimal.valueOf(item.getStockQuantity()));
    }

    public static BigDecimal subtotal(List<ProductItem> items) {
        BigDecimal result = cents(BigDecimal.ZERO);
        for (ProductItem item : items) result = result.add(lineGross(item));
        return result;
    }

    public static BigDecimal discountTotal(List<ProductItem> items) {
        BigDecimal result = cents(BigDecimal.ZERO);
        for (ProductItem item : items) result = result.add(lineDiscount(item));
        return result;
    }

    public static BigDecimal total(List<ProductItem> items) {
        return subtotal(items).subtract(discountTotal(items)).max(cents(BigDecimal.ZERO));
    }
}
