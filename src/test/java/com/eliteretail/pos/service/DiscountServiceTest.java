package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountServiceTest {
    private final DiscountService service = new DiscountService();

    @Test
    void percentageDiscountReturnsPerUnitAmount() {
        Discount discount = discount("PERCENT", 20, "");
        assertEquals(2.0, service.calculateDiscount(10.0, discount, 3), 0.0001);
    }

    @Test
    void fixedDiscountCannotExceedUnitPrice() {
        Discount discount = discount("FIXED_AMOUNT", 20, "");
        assertEquals(10.0, service.calculateDiscount(10.0, discount, 1), 0.0001);
    }

    @Test
    void buyTwoGetOneFreeProducesExpectedFreeUnitsAcrossQuantities() {
        Discount discount = discount("BUY_X_GET_Y", 0, "2+1");
        assertEquals(10.0, service.calculateDiscount(10.0, discount, 3) * 3, 0.0001);
        assertEquals(10.0, service.calculateDiscount(10.0, discount, 5) * 5, 0.0001);
        assertEquals(20.0, service.calculateDiscount(10.0, discount, 6) * 6, 0.0001);
        assertTrue(DiscountService.isValidBuyXGetYTarget("2+1"));
    }

    @Test
    void percentageCalculationCannotDiscountMoreThanUnitPrice() {
        Discount discount = discount("PERCENT", 150, "");
        assertEquals(10.0, service.calculateDiscount(10.0, discount, 1), 0.0001);
    }

    private Discount discount(String type, double value, String target) {
        Discount discount = new Discount(
                "Test", type, value, true,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 0, "ALL"
        );
        discount.setTargetValue(target);
        return discount;
    }
}
