package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.repository.DiscountRepository;

import com.eliteretail.pos.utils.MoneyAmounts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscountService {
    private static final Logger LOGGER = Logger.getLogger(DiscountService.class.getName());
    private final DiscountRepository discountRepository = new DiscountRepository();

    private void requireManager() {
        RoleGuard.requireManager(AuthService.getInstance().getCurrentUser());
    }

    public List<Discount> getAllDiscounts() {
        requireManager();
        return discountRepository.getAllDiscounts();
    }

    public Discount getDiscountById(int id) {
        requireManager();
        return discountRepository.getDiscountById(id);
    }

    public List<Discount> getActiveDiscounts() {
        RoleGuard.requireSignedIn(AuthService.getInstance().getCurrentUser());
        return discountRepository.getActiveDiscounts();
    }

    public List<String> getAllProductNames() {
        requireManager();
        return discountRepository.getAllProductNames();
    }

    public boolean addDiscount(Discount discount) {
        requireManager();
        validateDiscount(discount);
        return discountRepository.addDiscount(discount);
    }

    public boolean updateDiscount(Discount discount) {
        requireManager();
        validateDiscount(discount);
        return discountRepository.updateDiscount(discount);
    }

    public boolean deleteDiscount(int id) {
        requireManager();
        return discountRepository.deleteDiscount(id);
    }


    private void validateDiscount(Discount discount) {
        if (discount == null) {
            throw new IllegalArgumentException("Discount is required.");
        }
        if (discount.getName() == null || discount.getName().isBlank()) {
            throw new IllegalArgumentException("Discount name is required.");
        }
        if (discount.getStartDate() == null || discount.getEndDate() == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }
        if (discount.getStartDate().isAfter(discount.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date.");
        }
        if (discount.getUsageLimit() < 0) {
            throw new IllegalArgumentException("Usage limit cannot be negative.");
        }

        if ("BUY_X_GET_Y".equals(discount.getType())) {
            if (!isValidBuyXGetYTarget(discount.getTargetValue())) {
                throw new IllegalArgumentException("BUY_X_GET_Y target must use X+Y format, for example 2+1.");
            }
        } else if (discount.getValue() <= 0) {
            throw new IllegalArgumentException("Discount value must be positive.");
        } else if ("PERCENT".equals(discount.getType()) && discount.getValue() > 100) {
            throw new IllegalArgumentException("Percentage discounts cannot exceed 100%.");
        }

        if (discount.getMinQuantity() < 0) {
            throw new IllegalArgumentException("Minimum quantity cannot be negative.");
        }
        if (discount.getMinPurchaseAmount() < 0) {
            throw new IllegalArgumentException("Minimum purchase amount cannot be negative.");
        }
    }

    /**
     * Returns the discount amount per unit. The caller can multiply the result by quantity
     * to calculate the line-level discount consistently for every discount type.
     */
    public double calculateDiscount(double unitPrice, Discount discount, int quantity) {
        if (discount == null || !discount.isValid() || quantity <= 0) {
            return 0.0;
        }
        if (quantity < discount.getMinQuantity()) {
            return 0.0;
        }
        BigDecimal price = MoneyAmounts.cents(unitPrice);
        if (price.multiply(BigDecimal.valueOf(quantity))
                .compareTo(MoneyAmounts.cents(discount.getMinPurchaseAmount())) < 0) {
            return 0.0;
        }

        return switch (discount.getType()) {
            case "PERCENT" -> price.multiply(MoneyAmounts.exact(discount.getValue()))
                    .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
                    .min(price).doubleValue();
            case "FIXED_AMOUNT" -> MoneyAmounts.cents(discount.getValue()).min(price).doubleValue();
            case "BUY_X_GET_Y" -> calculateBuyXGetYPerUnit(price, discount, quantity);
            default -> 0.0;
        };
    }

    private double calculateBuyXGetYPerUnit(BigDecimal unitPrice, Discount discount, int quantity) {
        try {
            String[] parts = discount.getTargetValue().split("\\+");
            int buyX = Integer.parseInt(parts[0].trim());
            int getY = Integer.parseInt(parts[1].trim());
            int groupSize = buyX + getY;
            int freeItems = (quantity / groupSize) * getY;
            int remainder = quantity % groupSize;
            freeItems += Math.max(0, Math.min(getY, remainder - buyX));
            return unitPrice.multiply(BigDecimal.valueOf(freeItems))
                    .divide(BigDecimal.valueOf(quantity), 8, RoundingMode.HALF_UP).doubleValue();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Unable to calculate BUY_X_GET_Y discount.", e);
            return 0.0;
        }
    }

    public static boolean isValidBuyXGetYTarget(String targetValue) {
        if (targetValue == null || targetValue.isBlank()) {
            return false;
        }
        try {
            String[] parts = targetValue.split("\\+");
            if (parts.length != 2) {
                return false;
            }
            return Integer.parseInt(parts[0].trim()) > 0 && Integer.parseInt(parts[1].trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
