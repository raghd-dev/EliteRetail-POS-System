package com.eliteretail.pos.service;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.utils.MoneyAmounts;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MoneyAmountsTest {
    private ProductItem item(String code, double unitPrice, int quantity, double discount) {
        return new ProductItem(code, code, "", unitPrice, quantity, "Demo", discount);
    }

    @Test void decimalAdditionDoesNotLeakFloatingPointErrors() {
        List<ProductItem> cart = List.of(item("A", 0.10, 1, 0), item("B", 0.20, 1, 0));
        assertEquals(new BigDecimal("0.30"), MoneyAmounts.total(cart));
        assertEquals(0.30, MoneyAmounts.total(cart).doubleValue());
    }

    @Test void totalsAndDiscountsRoundAtCurrencyBoundary() {
        ProductItem item = item("A", 0.05, 3, 0.01);
        assertEquals(0.15, item.getTotalPrice());
        assertEquals(0.03, item.getTotalDiscount());
        assertEquals(0.12, item.getTotal());
    }

    @Test void cashInputRejectsFractionsOfCentsAndNonNumericValues() {
        assertEquals(new BigDecimal("0.30"), MoneyAmounts.parseCash("0.30"));
        assertEquals(new BigDecimal("12.00"), MoneyAmounts.parseCash("12"));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.parseCash("1.234"));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.parseCash("-1"));
        assertThrows(IllegalArgumentException.class, () -> MoneyAmounts.parseCash("NaN"));
    }

    @Test void validatorMatchesPersistedCentTotals() {
        List<ProductItem> cart = List.of(item("A", 0.10, 1, 0), item("B", 0.20, 1, 0));
        assertDoesNotThrow(() -> CheckoutValidator.validate(0.30, 0, PaymentMethod.CASH, 1, 0.70, cart));
        assertThrows(SQLException.class,
                () -> CheckoutValidator.validate(0.31, 0, PaymentMethod.CASH, 1, 0.69, cart));
    }

    @Test void percentagePromotionRoundsToUnitCentsBeforeSaving() {
        Discount discount = new Discount(5, "5%", "", 5, "PERCENT", "ALL", "",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1, 0, true, 0, 0);
        ProductItem item = item("A", 0.10, 3, 0);
        CartDiscountCalculator.apply(List.of(item), List.of(discount), new DiscountService());
        assertEquals(0.01, item.getDiscount());
        assertEquals(new BigDecimal("0.03"), MoneyAmounts.discountTotal(List.of(item)));
    }
}
