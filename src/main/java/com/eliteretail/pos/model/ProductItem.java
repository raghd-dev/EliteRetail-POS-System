package com.eliteretail.pos.model;

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
        this.unitPrice = unitPrice;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.discount = discount;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public double getTotalPrice() {
        return unitPrice * stockQuantity;
    }

    public double getDiscountedTotal() {
        return (unitPrice - discount) * stockQuantity;
    }

    public double getTotalDiscount() {
        return discount * stockQuantity;
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
