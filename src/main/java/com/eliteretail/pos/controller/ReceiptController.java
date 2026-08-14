package com.eliteretail.pos.controller;

import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.utils.ReceiptExporter;
import com.eliteretail.pos.utils.UiEffects;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReceiptController {

    @FXML private Label billNumberLabel;
    @FXML private Label cashierNameLabel;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label discountLabel;
    @FXML private Label totalLabel;
    @FXML private Label paymentMethodLabel;
    @FXML private Label receivedLabel;
    @FXML private Label changeLabel;

    @FXML private TableView<ProductItem> receiptTable;
    @FXML private TableColumn<ProductItem, String> codeCol;
    @FXML private TableColumn<ProductItem, String> nameCol;
    @FXML private TableColumn<ProductItem, Integer> qtyCol;
    @FXML private TableColumn<ProductItem, Double> priceCol;
    @FXML private TableColumn<ProductItem, Double> discountCol;
    @FXML private TableColumn<ProductItem, Double> totalCol;

    private final ObservableList<ProductItem> items = FXCollections.observableArrayList();

    private String currentBillNumber;
    private String currentCashierName;
    private List<ProductItem> currentPurchasedItems;
    private double currentSubtotal;
    private double currentDiscount;
    private double currentTotal;
    private String currentPaymentMethod;
    private double currentReceived;
    private double currentChange;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupDateTime();
    }
    private void setupTableColumns() {
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discount"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));
        receiptTable.setItems(items);
    }

    private void setupDateTime() {
        LocalDateTime now = LocalDateTime.now();
        dateLabel.setText(now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        timeLabel.setText(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    @FXML
    private void handlePrintReceipt() {
        Alert confirmAlert = new Alert(AlertType.CONFIRMATION);
        confirmAlert.setTitle("Export Receipt");
        confirmAlert.setHeaderText("Export to PDF");
        confirmAlert.setContentText("Do you want to export this receipt to PDF?");

        UiEffects.styleDialog(confirmAlert);
        Optional<ButtonType> result = confirmAlert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            exportToPDF();
        }
    }

    private void exportToPDF() {
        try {
            ReceiptExporter.exportReceiptToPDF(
                    currentBillNumber,
                    currentCashierName,
                    currentPurchasedItems,
                    currentSubtotal,
                    currentDiscount,
                    currentTotal,
                    currentPaymentMethod,
                    currentReceived,
                    currentChange
            );

        } catch (Exception e) {
            showAlert("Export Error",
                    "PDF Export Failed",
                    "An error occurred: " + e.getMessage());
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        UiEffects.styleDialog(alert);
        alert.showAndWait();
    }

    public void setReceiptData(
            String billNumber,
            String cashierName,
            List<ProductItem> purchasedItems,
            double subtotal,
            double discount,
            double total,
            String paymentMethod,
            double received,
            double change
    ) {
        storeReceiptData(billNumber, cashierName, purchasedItems, subtotal, discount, total, paymentMethod, received, change);
        updateUI(billNumber, cashierName, purchasedItems, subtotal, discount, total, paymentMethod, received, change);
    }

    private void storeReceiptData(
            String billNumber,
            String cashierName,
            List<ProductItem> purchasedItems,
            double subtotal,
            double discount,
            double total,
            String paymentMethod,
            double received,
            double change
    ) {
        this.currentBillNumber = billNumber;
        this.currentCashierName = cashierName;
        this.currentPurchasedItems = new ArrayList<>(purchasedItems);
        this.currentSubtotal = subtotal;
        this.currentDiscount = discount;
        this.currentTotal = total;
        this.currentPaymentMethod = paymentMethod;
        this.currentReceived = received;
        this.currentChange = change;
    }

    private void updateUI(
            String billNumber,
            String cashierName,
            List<ProductItem> purchasedItems,
            double subtotal,
            double discount,
            double total,
            String paymentMethod,
            double received,
            double change
    ) {
        billNumberLabel.setText(billNumber);
        cashierNameLabel.setText(cashierName);

        items.setAll(purchasedItems);

        subtotalLabel.setText(String.format("$%.2f", subtotal));
        discountLabel.setText(String.format("$%.2f", discount));
        totalLabel.setText(String.format("$%.2f", total));

        paymentMethodLabel.setText("Paid by: " + paymentMethod);
        receivedLabel.setText(String.format("$%.2f", received));
        changeLabel.setText(String.format("$%.2f", change));

        receiptTable.refresh();
    }

}
