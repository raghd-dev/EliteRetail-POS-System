package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.model.ProductItem;

import com.eliteretail.pos.utils.MoneyAmounts;

import java.math.BigDecimal;
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
            BigDecimal perUnitDiscount = BigDecimal.ZERO;
            Set<Integer> lineDiscountIds = new LinkedHashSet<>();
            for (Discount discount : activeDiscounts) {
                if (!"ALL".equals(discount.getApplyTo()) &&
                        !item.getName().equals(discount.getApplyTo())) {
                    continue;
                }
                BigDecimal proposed = MoneyAmounts.exact(discountService.calculateDiscount(
                        item.getUnitPrice(), discount, item.getStockQuantity())).max(BigDecimal.ZERO);
                BigDecimal remaining = MoneyAmounts.cents(item.getUnitPrice()).subtract(perUnitDiscount)
                        .max(BigDecimal.ZERO);
                BigDecimal contribution = proposed.min(remaining);
                if (contribution.signum() > 0) {
                    if (discount.getId() <= 0) {
                        throw new IllegalArgumentException("Applied discount must have a database ID.");
                    }
                    perUnitDiscount = perUnitDiscount.add(contribution);
                    lineDiscountIds.add(discount.getId());
                }
            }
            // bill_items.discount is DECIMAL(12,2), so round per unit here,
            // before calculating totals or persisting the sale.
            item.setDiscount(MoneyAmounts.cents(perUnitDiscount).doubleValue());
            if (item.getDiscount() > 0) appliedIds.addAll(lineDiscountIds);
        }
        return appliedIds;
    }
}
