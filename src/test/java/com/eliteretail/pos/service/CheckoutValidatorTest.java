package com.eliteretail.pos.service;

import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutValidatorTest {
    private static ProductItem product(int quantity, double unitPrice, double unitDiscount) {
        return new ProductItem("P001", "Demo product", "", unitPrice, quantity, "Demo", unitDiscount);
    }

    @Test void validCashAndCardSales() {
        assertDoesNotThrow(() -> CheckoutValidator.validate(5.00, 1.00, PaymentMethod.CASH,
                10.00, 5.00, List.of(product(2, 3.00, 0.50))));
        assertDoesNotThrow(() -> CheckoutValidator.validate(5.00, 1.00, PaymentMethod.DEBIT_CARD,
                5.00, 0.00, List.of(product(2, 3.00, 0.50))));
    }

    @Test void rejectNonFiniteAndInvalidCash() {
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                Double.NaN, 0, List.of(product(2, 3, 0.5))));
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                4, 0, List.of(product(2, 3, 0.5))));
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                10, 4, List.of(product(2, 3, 0.5))));
    }

    @Test void rejectInvalidCartOrTotals() {
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(6, 1, PaymentMethod.CASH,
                10, 4, List.of(product(2, 3, 0.5))));
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                10, 5, List.of(product(0, 3, 0.5))));
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                10, 5, List.of(product(1, 3, 0.5), product(1, 3, 0.5))));
    }

    @Test void rejectInvalidCardAndMissingItems() {
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.MOBILE_PAY,
                0, 0, List.of(product(2, 3, 0.5))));
        assertThrows(SQLException.class, () -> CheckoutValidator.validate(5, 1, PaymentMethod.CASH,
                10, 5, List.of()));
    }
}
