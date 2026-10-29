package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.model.ProductItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CartDiscountCalculatorTest {
    private final DiscountService service = new DiscountService();

    @Test
    void discountOnTwoProductsCountsOncePerSale() {
        Discount discount = discount(7, "ALL", 10);
        ProductItem milk = product("Milk");
        ProductItem bread = product("Bread");
        assertEquals(Set.of(7), CartDiscountCalculator.apply(
                List.of(milk, bread), List.of(discount), service));
        assertEquals(1.0, milk.getDiscount(), 0.0001);
        assertEquals(1.0, bread.getDiscount(), 0.0001);
    }

    @Test
    void fullyShadowedDiscountIsNotConsumed() {
        ProductItem item = product("Milk");
        Discount first = discount(7, "ALL", 100);
        Discount second = discount(8, "ALL", 10);
        assertEquals(Set.of(7), CartDiscountCalculator.apply(
                List.of(item), List.of(first, second), service));
        assertEquals(10.0, item.getDiscount(), 0.0001);
    }

    @Test
    void unrelatedDiscountIsNotConsumed() {
        ProductItem item = product("Milk");
        Discount onlyBread = discount(9, "Bread", 50);
        assertEquals(Set.of(), CartDiscountCalculator.apply(
                List.of(item), List.of(onlyBread), service));
        assertEquals(0.0, item.getDiscount(), 0.0001);
    }

    private ProductItem product(String name) {
        return new ProductItem(name, name, "", 10.0, 2, "Test");
    }

    private Discount discount(int id, String target, double percent) {
        return new Discount(id, "Test promotion", "", percent, "PERCENT", target, "",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1),
                1, 0, true, 0, 0);
    }
}
