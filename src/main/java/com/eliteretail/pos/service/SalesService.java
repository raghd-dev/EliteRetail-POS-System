package com.eliteretail.pos.service;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.model.User;
import com.eliteretail.pos.repository.BillRepository;
import com.eliteretail.pos.repository.DiscountRepository;
import com.eliteretail.pos.repository.ProductRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

public class SalesService {
    private static final Logger LOGGER = Logger.getLogger(SalesService.class.getName());

    public record SaleResult(int billId, String billNumber, int serialNumber) {}
    private final BillRepository billRepository = new BillRepository();
    private final ProductRepository productRepository = new ProductRepository();
    private final DiscountRepository discountRepository = new DiscountRepository();

    public int getNextDailySerial(String machineNumber) throws SQLException {
        RoleGuard.requireCashier(AuthService.getInstance().getCurrentUser());
        return billRepository.getNextDailySerial(machineNumber);
    }

    /**
     * Receipts are allocated only when the sale is committed; any displayed
     * number before payment is a preview and may differ under concurrency.
     */
    public SaleResult recordSale(
            String cashierCode,
            String cashierName,
            String machineNumber,
            double totalAmount,
            double totalDiscount,
            PaymentMethod paymentMethod,
            double amountReceived,
            double changeAmount,
            List<ProductItem> items,
            Set<Integer> appliedDiscountIds
    ) throws SQLException {
        User cashier = AuthService.getInstance().getCurrentUser();
        RoleGuard.requireCashier(cashier);
        if (!cashier.getCashierCode().equals(cashierCode)) {
            throw new SecurityException("The sale must be recorded under the signed-in cashier.");
        }
        // Use the authenticated cashier's name, not caller-provided display text.
        cashierName = cashier.getCashierName();
        CheckoutValidator.validate(totalAmount, totalDiscount, paymentMethod,
                amountReceived, changeAmount, items);
        try (Connection connection = DatabaseConnector.getConnection()) {
            // Each checkout owns a fresh connection, which is closed after the sale.
            connection.setAutoCommit(false);
            boolean lockAcquired = false;
            try {
                billRepository.acquireSerialLock(connection, machineNumber);
                lockAcquired = true;
                BillRepository.BillNumber number = billRepository.allocateBillNumber(connection, machineNumber);
                int billId = billRepository.insertBill(
                        connection, cashierCode, cashierName, number.value(), machineNumber, number.serial(),
                        totalAmount, totalDiscount, paymentMethod, amountReceived, changeAmount
                );
                billRepository.insertItems(connection, billId, items);
                productRepository.decrementStock(connection, items);
                discountRepository.incrementUsage(connection, appliedDiscountIds);
                connection.commit();
                return new SaleResult(billId, number.value(), number.serial());
            } catch (SQLException | RuntimeException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            } finally {
                if (lockAcquired) {
                    try {
                        // User-level MySQL locks are NOT released by COMMIT or ROLLBACK.
                        billRepository.releaseSerialLock(connection, machineNumber);
                    } catch (SQLException releaseError) {
                        // Closing the JDBC connection below also releases this session lock.
                        LOGGER.log(Level.WARNING, "Unable to explicitly release bill serial lock.", releaseError);
                    }
                }
            }
        }
    }
}
