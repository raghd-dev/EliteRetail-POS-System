package com.eliteretail.pos.model;

import com.eliteretail.pos.utils.MoneyAmounts;

public class ProductItem {
    private String code;
    private String name;
    private String description;
    private double unitPrice;
    private int stockQuantity;
    private String category;
    private double discount;

    public ProductItem(String code, String name, String description, double unitPrice,
                       int stockQuantity, String category) {
        this(code, name, description, unitPrice, stockQuantity, category, 0.0);
    }

    public ProductItem(String code, String name, String description, double unitPrice,
                       int stockQuantity, String category, double discount) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.unitPrice = MoneyAmounts.cents(unitPrice).doubleValue();
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.discount = MoneyAmounts.cents(discount).doubleValue();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = MoneyAmounts.cents(unitPrice).doubleValue(); }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = MoneyAmounts.cents(discount).doubleValue(); }

    public double getTotalPrice() {
        return MoneyAmounts.cents(unitPrice).multiply(java.math.BigDecimal.valueOf(stockQuantity)).doubleValue();
    }

    public double getDiscountedTotal() {
        return MoneyAmounts.cents(unitPrice).subtract(MoneyAmounts.cents(discount))
                .multiply(java.math.BigDecimal.valueOf(stockQuantity)).doubleValue();
    }

    public double getTotalDiscount() {
        return MoneyAmounts.cents(discount).multiply(java.math.BigDecimal.valueOf(stockQuantity)).doubleValue();
    }

    public double getTotal() {
        return getDiscountedTotal();
    }

    public boolean hasDiscount() {
        return discount > 0.0;
    }

    public double getDiscountPercentage() {
        return unitPrice == 0.0 ? 0.0 : (discount / unitPrice) * 100.0;
    }

    @Override
    public String toString() {
        return String.format("%s - %s (Category: %s)", code, name, category);
    }
}
