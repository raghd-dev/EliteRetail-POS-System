package com.eliteretail.pos.model;

import java.util.ArrayList;
import java.util.List;

public enum ProductCategory {
    DAIRY("1000", "Dairy & Eggs"),
    BAKERY("2000", "Bakery & Bread"),
    BEVERAGES("3000", "Beverages"),
    SNACKS("4000", "Snacks & Chips"),
    CANNED_GOODS("5000", "Canned Goods"),
    FROZEN_FOODS("6000", "Frozen Foods"),
    MEAT("7000", "Meat & Poultry"),
    PRODUCE("8000", "Fruits & Vegetables"),
    CLEANING("9000", "Cleaning Supplies"),
    PERSONAL_CARE("9100", "Personal Care");

    private static final int CUSTOM_PREFIX_STEP = 100;
    private static final List<CustomCategory> CUSTOM_CATEGORIES = new ArrayList<>();

    private final String codePrefix;
    private final String displayName;

    ProductCategory(String codePrefix, String displayName) {
        this.codePrefix = codePrefix;
        this.displayName = displayName;
    }

    public String getCodePrefix() {
        return codePrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static Object[] getAllCategories() {
        List<Object> categories = new ArrayList<>(List.of(values()));
        categories.addAll(CUSTOM_CATEGORIES);
        return categories.toArray();
    }

    public static void addCustomCategory(String displayName, String codePrefix) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Category name is required.");
        }
        if (codePrefix == null || !codePrefix.matches("\\d{4}")) {
            throw new IllegalArgumentException("Category code prefix must contain exactly four digits.");
        }
        boolean duplicateBuiltInPrefix = List.of(values()).stream()
                .anyMatch(category -> category.codePrefix.equals(codePrefix));
        boolean duplicateCustomPrefix = CUSTOM_CATEGORIES.stream()
                .anyMatch(category -> category.codePrefix.equals(codePrefix));
        if (duplicateBuiltInPrefix || duplicateCustomPrefix) {
            throw new IllegalArgumentException("Category code prefix already exists.");
        }
        CUSTOM_CATEGORIES.add(new CustomCategory(displayName.trim(), codePrefix));
    }

    public static Object fromCode(String code) {
        if (code == null || code.length() < 4) {
            return null;
        }

        String prefix = code.substring(0, 4);
        for (ProductCategory category : values()) {
            if (category.codePrefix.equals(prefix)) {
                return category;
            }
        }
        return CUSTOM_CATEGORIES.stream()
                .filter(category -> category.codePrefix.equals(prefix))
                .findFirst()
                .orElse(null);
    }

    public static String getNextAvailablePrefix() {
        int maxPrefix = 0;
        for (ProductCategory category : values()) {
            maxPrefix = Math.max(maxPrefix, parsePrefix(category.codePrefix));
        }
        for (CustomCategory category : CUSTOM_CATEGORIES) {
            maxPrefix = Math.max(maxPrefix, parsePrefix(category.codePrefix));
        }
        return String.format("%04d", maxPrefix + CUSTOM_PREFIX_STEP);
    }

    public static void renameCustomCategory(Object category, String newName) {
        if (category instanceof CustomCategory customCategory) {
            customCategory.setDisplayName(newName);
        }
    }

    public static void removeCustomCategory(Object category) {
        if (category instanceof CustomCategory) {
            CUSTOM_CATEGORIES.remove(category);
        }
    }

    private static int parsePrefix(String prefix) {
        try {
            return Integer.parseInt(prefix);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static final class CustomCategory {
        private String displayName;
        private final String codePrefix;

        private CustomCategory(String displayName, String codePrefix) {
            this.displayName = displayName;
            this.codePrefix = codePrefix;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCodePrefix() {
            return codePrefix;
        }

        public void setDisplayName(String newName) {
            if (newName == null || newName.isBlank()) {
                throw new IllegalArgumentException("Category name is required.");
            }
            this.displayName = newName.trim();
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
