package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.model.ProductItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Computes cart discount previews and identifies promotions that actually reduced a price. */
public final class CartDiscountCalculator {
    private CartDiscountCalculator() {}

    /**
     * Sets each cart item's per-unit discount, returning the IDs of discounts
     * that actually contributed savings. An ID occurs only once per sale.
     */
    public static Set<Integer> apply(List<ProductItem> cartItems,
                                     List<Discount> activeDiscounts,
                                     DiscountService discountService) {
        Set<Integer> appliedIds = new LinkedHashSet<>();
        for (ProductItem item : cartItems) {
            double perUnitDiscount = 0.0;
            for (Discount discount : activeDiscounts) {
                if (!"ALL".equals(discount.getApplyTo()) &&
                        !item.getName().equals(discount.getApplyTo())) {
                    continue;
                }
                double proposed = discountService.calculateDiscount(
                        item.getUnitPrice(), discount, item.getStockQuantity());
                double remaining = Math.max(0.0, item.getUnitPrice() - perUnitDiscount);
                double contribution = Math.min(remaining, Math.max(0.0, proposed));
                if (contribution > 1e-9) {
                    if (discount.getId() <= 0) {
                        throw new IllegalArgumentException("Applied discount must have a database ID.");
                    }
                    perUnitDiscount += contribution;
                    appliedIds.add(discount.getId());
                }
            }
            item.setDiscount(perUnitDiscount);
        }
        return appliedIds;
    }
}
