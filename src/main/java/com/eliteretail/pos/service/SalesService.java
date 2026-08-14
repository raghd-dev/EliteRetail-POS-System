package com.eliteretail.pos.service;

import com.eliteretail.pos.database.DatabaseConnector;
import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.repository.BillRepository;
import com.eliteretail.pos.repository.ProductRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class SalesService {
    private final BillRepository billRepository = new BillRepository();
    private final ProductRepository productRepository = new ProductRepository();

    public int getNextDailySerial(String machineNumber) throws SQLException {
        return billRepository.getNextDailySerial(machineNumber);
    }

    public int recordSale(
            String cashierCode,
            String cashierName,
            String billNumber,
            String machineNumber,
            int serialNumber,
            double totalAmount,
            double totalDiscount,
            PaymentMethod paymentMethod,
            double amountReceived,
            double changeAmount,
            List<ProductItem> items
    ) throws SQLException {
        try (Connection connection = DatabaseConnector.getConnection()) {
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int billId = billRepository.insertBill(
                        connection, cashierCode, cashierName, billNumber, machineNumber, serialNumber,
                        totalAmount, totalDiscount, paymentMethod, amountReceived, changeAmount
                );
                billRepository.insertItems(connection, billId, items);
                productRepository.decrementStock(connection, items);
                connection.commit();
                return billId;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        }
    }
}
