package com.eliteretail.pos.model;

import java.time.LocalDate;

public class Discount {
    private int id;
    private String name;
    private String description;
    private double value;
    private String type;
    private String applyTo;
    private String targetValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private int minQuantity;
    private double minPurchaseAmount;
    private boolean active;
    private int usageLimit;
    private int timesUsed;

    public Discount(String name, String type, double value, boolean active,
                    LocalDate startDate, LocalDate endDate, int usageLimit, String applyTo) {
        this(0, name, "", value, type, applyTo, "", startDate, endDate,
                1, 0.0, active, usageLimit, 0);
    }

    public Discount(int id, String name, String description, double value, String type,
                    String applyTo, String targetValue, LocalDate startDate, LocalDate endDate,
                    int minQuantity, double minPurchaseAmount, boolean active,
                    int usageLimit, int timesUsed) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.value = value;
        this.type = type;
        this.applyTo = applyTo;
        this.targetValue = targetValue;
        this.startDate = startDate;
        this.endDate = endDate;
        this.minQuantity = minQuantity;
        this.minPurchaseAmount = minPurchaseAmount;
        this.active = active;
        this.usageLimit = usageLimit;
        this.timesUsed = timesUsed;
    }

    public Discount(String name, String type, double value, boolean active,
                    LocalDate startDate, LocalDate endDate, int usageLimit) {
        this(name, type, value, active, startDate, endDate, usageLimit, "ALL");
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getApplyTo() { return applyTo; }
    public void setApplyTo(String applyTo) { this.applyTo = applyTo; }
    public String getTargetValue() { return targetValue; }
    public void setTargetValue(String targetValue) { this.targetValue = targetValue; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public double getMinPurchaseAmount() { return minPurchaseAmount; }
    public void setMinPurchaseAmount(double minPurchaseAmount) { this.minPurchaseAmount = minPurchaseAmount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { this.usageLimit = usageLimit; }
    public int getTimesUsed() { return timesUsed; }
    public void setTimesUsed(int timesUsed) { this.timesUsed = timesUsed; }

    public boolean isValid() {
        if (!active || startDate == null || endDate == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate)
                && !today.isAfter(endDate)
                && (usageLimit == 0 || timesUsed < usageLimit);
    }

    @Override
    public String toString() {
        String discountText;
        if ("BUY_X_GET_Y".equals(type)) {
            discountText = formatBuyXGetY();
        } else if ("PERCENT".equals(type)) {
            discountText = value + "%";
        } else {
            discountText = "$" + value;
        }

        String productText = applyTo == null || "ALL".equals(applyTo) ? "" : " for " + applyTo;
        return name + " (" + discountText + productText + ") - " + (isValid() ? "Active" : "Inactive");
    }

    private String formatBuyXGetY() {
        if (targetValue == null) {
            return "Buy X Get Y";
        }
        String[] parts = targetValue.split("\\+");
        if (parts.length != 2) {
            return targetValue;
        }
        return "Buy " + parts[0].trim() + " Get " + parts[1].trim();
    }
}
