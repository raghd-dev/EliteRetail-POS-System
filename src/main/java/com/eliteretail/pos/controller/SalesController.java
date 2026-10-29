package com.eliteretail.pos.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import com.eliteretail.pos.service.DiscountService;
import com.eliteretail.pos.service.CartDiscountCalculator;
import com.eliteretail.pos.service.ProductService;
import com.eliteretail.pos.service.SalesService;
import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.model.PaymentMethod;
import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.utils.UiEffects;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

public class SalesController {

    private static final String MACHINE_NUMBER = "M001";

    @FXML private Label cashierNameLabel;
    @FXML private Label cashierCodeLabel;
    @FXML private Label timeLabel;
    @FXML private Label machineStatusLabel;
    @FXML private Label billSerialLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label dateLabel;
    @FXML private Label cartCountLabel;
    @FXML private ComboBox<ProductItem> productComboBox;
    @FXML private TextField amountField;

    @FXML private TableView<ProductItem> productTable;
    @FXML private TableColumn<ProductItem, String> codeCol;
    @FXML private TableColumn<ProductItem, String> nameCol;
    @FXML private TableColumn<ProductItem, Integer> amountCol;
    @FXML private TableColumn<ProductItem, Double> priceCol;
    @FXML private TableColumn<ProductItem, Double> discountCol;
    @FXML private TableColumn<ProductItem, Double> totalCol;

    private final ObservableList<ProductItem> cartItems = FXCollections.observableArrayList();
    private final DiscountService discountService = new DiscountService();
    private final ProductService productService = new ProductService();
    private final SalesService salesService = new SalesService();
    private double totalDiscount = 0.0;
    private final Set<Integer> appliedDiscountIds = new LinkedHashSet<>();
    private Timeline clock;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupProductComboBox();
        startClock();
        updateBillSerial();
        setupRealTimeCalculations();
        loadProductsToComboBox();
        setupCartCounter();
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Logout");
        confirm.setHeaderText("Are you sure you want to logout from cashier mode?");
        confirm.setContentText("You'll be returned to the main dashboard.");
        UiEffects.styleDialog(confirm);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                    Parent root = loader.load();
                    DashboardController dashboardController = loader.getController();
                    dashboardController.initDashboard();
                    dashboardController.setWelcomeMessage(cashierNameLabel.getText());
                    UiEffects.prepare(root);

                    if (clock != null) {
                        clock.stop();
                    }
                    Stage stage = (Stage) cashierNameLabel.getScene().getWindow();
                    UiEffects.applyStageBranding(stage);
                    stage.setTitle("EliteRetail POS - Dashboard");
                    stage.setScene(new Scene(root, 1180, 760));
                    stage.show();
                } catch (Exception e) {
                    showAlert("Error", "Could not return to dashboard: " + e.getMessage());
                }
            }
        });
    }
    /* ================= CLOCK & DATE ================= */
    private void startClock() {
        if (clock != null) {
            clock.stop();
        }
        clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            updateClock();
            updateDate();
        }), new KeyFrame(Duration.seconds(1)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void updateClock() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        timeLabel.setText(LocalDateTime.now().format(formatter));
    }

    private void updateDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy");
        dateLabel.setText(LocalDateTime.now().format(formatter));
    }

    /* ================= CART COUNTER ================= */
    private void setupCartCounter() {
        cartItems.addListener((ListChangeListener<ProductItem>) change -> updateCartCount());
        updateCartCount();
    }

    private void updateCartCount() {
        int totalItems = cartItems.stream().mapToInt(ProductItem::getStockQuantity).sum();
        cartCountLabel.setText("Items: " + totalItems);

        if (totalItems > 0) {
            machineStatusLabel.setText("ACTIVE SALE");
        } else {
            machineStatusLabel.setText("READY");
        }
    }

    /* ================= TABLE ================= */
    private void setupTableColumns() {
        codeCol.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        discountCol.setCellValueFactory(new PropertyValueFactory<>("discount"));
        totalCol.setCellValueFactory(cellData -> {
            ProductItem item = cellData.getValue();
            return new javafx.beans.property.SimpleDoubleProperty(item.getTotal()).asObject();
        });

        amountCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        amountCol.setOnEditCommit(event -> {
            ProductItem item = event.getRowValue();
            int newQuantity = event.getNewValue();

            if (newQuantity <= 0) {
                showAlert("Error", "Quantity must be positive");
                productTable.refresh();
                return;
            }

            if (!validateStockAvailability(item, newQuantity)) {
                ProductItem dbProduct = productService.findProductByCode(item.getCode());
                int availableStock = dbProduct != null ? dbProduct.getStockQuantity() : 0;
                showAlert("Error", "Not enough stock. Only " + availableStock + " available");
                productTable.refresh();
                return;
            }

            item.setStockQuantity(newQuantity);
            applyDiscountsToCart();
            updateTotals();
            productTable.refresh();
        });

        productTable.setEditable(true);
        productTable.setItems(cartItems);
    }

    /* ================= PRODUCT COMBOBOX ================= */
    private void setupProductComboBox() {
        productComboBox.setCellFactory(lv -> new ListCell<ProductItem>() {
            @Override
            protected void updateItem(ProductItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " - $" + item.getUnitPrice() + " (" + item.getStockQuantity() + " in stock)");
                }
            }
        });

        productComboBox.setButtonCell(new ListCell<ProductItem>() {
            @Override
            protected void updateItem(ProductItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select Product");
                } else {
                    setText(item.getName() + " - $" + item.getUnitPrice());
                }
            }
        });
    }

    private void loadProductsToComboBox() {
        ObservableList<ProductItem> products = loadProductsFromDatabase();
        productComboBox.getItems().setAll(products);
    }

    private ObservableList<ProductItem> loadProductsFromDatabase() {
        return FXCollections.observableArrayList(productService.getAvailableProducts());
    }

    /* ================= BILL NUMBER ================= */
    private String generateBillNumber() {
        String machineNumber = MACHINE_NUMBER;
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int dailySerial = getNextDailySerial(machineNumber);

        return String.format("%s-%s-%04d", machineNumber, datePart, dailySerial);
    }

    private int getNextDailySerial(String machineNumber) {
        try {
            return salesService.getNextDailySerial(machineNumber);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not fetch the next bill number. Using serial 1 for this session.");
            return 1;
        }
    }

    private void updateBillSerial() {
        String billNumber = generateBillNumber();
        billSerialLabel.setText(billNumber);
    }

    /* ================= TOTALS & DISCOUNTS ================= */
    private void setupRealTimeCalculations() {
        cartItems.addListener((ListChangeListener<ProductItem>) change -> applyDiscountsToCart());
    }

    private void updateTotals() {
        double subtotal = calculateSubtotal();
        double finalTotal = calculateFinalTotal();
        totalPriceLabel.setText(String.format("$%.2f", finalTotal));

    }

    private double calculateSubtotal() {
        return cartItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getStockQuantity())
                .sum();
    }

    private double calculateFinalTotal() {
        return Math.max(0.0, calculateSubtotal() - totalDiscount);
    }

    private boolean validateStockAvailability(ProductItem product, int quantity) {
        ProductItem dbProduct = productService.findProductByCode(product.getCode());
        return dbProduct != null && dbProduct.getStockQuantity() >= quantity;
    }

    private void applyDiscountsToCart() {
        List<Discount> activeDiscounts = discountService.getActiveDiscounts();
        appliedDiscountIds.clear();
        appliedDiscountIds.addAll(CartDiscountCalculator.apply(cartItems, activeDiscounts, discountService));
        totalDiscount = cartItems.stream().mapToDouble(ProductItem::getTotalDiscount).sum();
        productTable.refresh();
        updateTotals();
    }
    /* ================= CART ACTIONS ================= */
    @FXML
    private void handleAddToCart() {
        ProductItem selected = productComboBox.getValue();
        if (selected == null) {
            showAlert("Error", "Please select a product");
            return;
        }

        String amountText = amountField.getText();
        if (amountText.isEmpty()) {
            showAlert("Error", "Please enter quantity");
            return;
        }

        try {
            int quantity = Integer.parseInt(amountText);
            if (quantity <= 0) {
                showAlert("Error", "Quantity must be positive");
                return;
            }

            if (!validateStockAvailability(selected, quantity)) {
                ProductItem dbProduct = productService.findProductByCode(selected.getCode());
                int availableStock = dbProduct != null ? dbProduct.getStockQuantity() : 0;
                showAlert("Error", "Not enough stock. Only " + availableStock + " available");
                return;
            }

            ProductItem cartItem = new ProductItem(
                    selected.getCode(),
                    selected.getName(),
                    selected.getDescription(),
                    selected.getUnitPrice(),
                    quantity,
                    selected.getCategory()
            );

            Optional<ProductItem> existingItem = cartItems.stream()
                    .filter(item -> item.getCode().equals(cartItem.getCode()))
                    .findFirst();

            if (existingItem.isPresent()) {
                ProductItem item = existingItem.get();
                int newQuantity = item.getStockQuantity() + quantity;
                if (newQuantity > selected.getStockQuantity()) {
                    showAlert("Error", "Cannot add more than available stock");
                    return;
                }
                item.setStockQuantity(newQuantity);
            } else {
                cartItems.add(cartItem);
            }

            applyDiscountsToCart();
            updateTotals();
            amountField.clear();
            productTable.refresh();

        } catch (NumberFormatException e) {
            showAlert("Error", "Please enter a valid number");
        }
    }

    @FXML
    private void handlePayClick() {
        if (cartItems.isEmpty()) {
            showAlert("Error", "Cart is empty");
            return;
        }

        try {
            // Refresh promotions just before collecting payment.
            applyDiscountsToCart();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/payment-dialog.fxml"));
            Parent root = loader.load();
            UiEffects.prepare(root);

            PaymentDialogController controller = loader.getController();
            controller.setAmountDue(calculateFinalTotal());

            Stage dialogStage = new Stage();
            dialogStage.setTitle("EliteRetail POS - Payment");
            UiEffects.applyStageBranding(dialogStage);
            dialogStage.setScene(new Scene(root));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

            PaymentDialogController.PaymentResult result = controller.getResult();
            if (result != null) {
                processPayment(result);
            }
        } catch (Exception e) {
            showAlert("Error", "Could not open payment dialog: " + e.getMessage());
        }
    }

    private void processPayment(PaymentDialogController.PaymentResult payment) {
        String method = payment.getMethod();

        if ("CASH".equals(method)) {
            completePayment(PaymentMethod.CASH, payment.getAmountDue(),
                    payment.getReceived(), payment.getChange());
        } else if ("CREDIT_CARD".equals(method)) {
            completePayment(PaymentMethod.CREDIT_CARD, payment.getAmountDue(), payment.getAmountDue(), 0);
        } else if ("DEBIT_CARD".equals(method)) {
            completePayment(PaymentMethod.DEBIT_CARD, payment.getAmountDue(), payment.getAmountDue(), 0);
        } else if ("MOBILE_PAY".equals(method)) {
            completePayment(PaymentMethod.MOBILE_PAY, payment.getAmountDue(), payment.getAmountDue(), 0);
        }
    }


    private void completePayment(PaymentMethod method, double amount, double received, double change) {
        try {
            String billNumber = billSerialLabel.getText();
            int serialNumber = Integer.parseInt(billNumber.substring(billNumber.lastIndexOf('-') + 1));

            salesService.recordSale(
                    cashierCodeLabel.getText(),
                    cashierNameLabel.getText(),
                    billNumber,
                    MACHINE_NUMBER,
                    serialNumber,
                    amount,
                    totalDiscount,
                    method,
                    received,
                    change,
                    List.copyOf(cartItems),
                    Set.copyOf(appliedDiscountIds)
            );

            showReceipt(method, amount, received, change);
            resetPageForNextCustomer();
        } catch (SQLException | NumberFormatException e) {
            applyDiscountsToCart();
            showAlert("Transaction Error", "The sale could not be completed. No inventory changes were committed. " + e.getMessage());
        }
    }

    private void resetPageForNextCustomer() {
        cartItems.clear();
        appliedDiscountIds.clear();
        totalDiscount = 0.0;
        updateBillSerial();
        updateTotals();
        loadProductsToComboBox();
        if (productTable != null) productTable.getSelectionModel().clearSelection();
        if (productComboBox != null) productComboBox.getSelectionModel().clearSelection();
        if (amountField != null) amountField.clear();
        updateCartCount();
    }

    @FXML
    private void handleClearCart() {
        if (!cartItems.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Clear Cart");
            confirm.setHeaderText("Are you sure you want to clear the cart?");
            confirm.setContentText("This action cannot be undone.");
            UiEffects.styleDialog(confirm);

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    cartItems.clear();
                    totalDiscount = 0.0;
                                updateTotals();
                }
            });
        }
    }

    /* ================= RECEIPT ================= */
    private void showReceipt(PaymentMethod method, double amount, double received, double change) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/receipt.fxml"));
            Parent root = loader.load();
            UiEffects.prepare(root);

            ReceiptController controller = loader.getController();

            String billNumber = billSerialLabel.getText();

            controller.setReceiptData(
                    billNumber,
                    cashierNameLabel.getText(),
                    cartItems,
                    calculateSubtotal(),
                    totalDiscount,
                    amount,
                    method.toString(),
                    received,
                    change
            );

            Stage receiptStage = new Stage();
            receiptStage.setTitle("EliteRetail POS - Receipt");
            UiEffects.applyStageBranding(receiptStage);
            receiptStage.setScene(new Scene(root, 520, 720));
            receiptStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            receiptStage.show();

        } catch (Exception e) {
            showAlert("Error", "Could not load receipt: " + e.getMessage());
        }
    }

    /* ================= HELPERS ================= */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        UiEffects.styleDialog(alert);
        alert.showAndWait();
    }

    public void setCashierInfo(String name, String code) {
        if (cashierNameLabel != null) cashierNameLabel.setText(name);
        if (cashierCodeLabel != null) cashierCodeLabel.setText(code);
    }

}
