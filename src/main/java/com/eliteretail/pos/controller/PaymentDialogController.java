package com.eliteretail.pos.controller;

import com.eliteretail.pos.utils.UiEffects;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PaymentDialogController {

    @FXML private ComboBox<String> methodComboBox;
    @FXML private Label amountDueLabel;
    @FXML private TextField amountReceivedField;
    @FXML private Label changeLabel;
    @FXML private Button confirmButton;
    @FXML private Button cancelButton;
    @FXML private VBox cashSection;

    private double amountDue;
    private PaymentResult result;

    @FXML
    public void initialize() {
        methodComboBox.getItems().addAll("CASH", "CREDIT_CARD", "DEBIT_CARD", "MOBILE_PAY");
        methodComboBox.getSelectionModel().selectFirst();

        confirmButton.setOnAction(e -> handleConfirm());
        cancelButton.setOnAction(e -> handleCancel());

        amountReceivedField.textProperty().addListener((obs, oldVal, newVal) -> updateChange());

        updatePaymentMethodUI();

        methodComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePaymentMethodUI());
    }

    private void updatePaymentMethodUI() {
        String method = methodComboBox.getValue();
        boolean isCash = "CASH".equals(method);

        if (cashSection != null) {
            cashSection.setVisible(isCash);
            cashSection.setManaged(isCash);
        }

        if (isCash && amountReceivedField != null) {
            amountReceivedField.requestFocus();
        }
    }

    public void setAmountDue(double amount) {
        this.amountDue = amount;
        amountDueLabel.setText(String.format("$%.2f", amount));
    }

    private void updateChange() {
        try {
            double received = Double.parseDouble(amountReceivedField.getText());
            double change = received - amountDue;
            changeLabel.setText(String.format("$%.2f", Math.max(change, 0)));

            if (change < 0) {
                changeLabel.setStyle("-fx-text-fill: #B4232D; -fx-font-size: 16px;");
            } else {
                changeLabel.setStyle("-fx-text-fill: #2F855A; -fx-font-size: 16px;");
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("$0.00");
            changeLabel.setStyle("-fx-text-fill: #617087; -fx-font-size: 16px;");
        }
    }

    private void handleConfirm() {
        String method = methodComboBox.getValue();
        double received = 0;
        double change = 0;

        if ("CASH".equals(method)) {
            try {
                received = Double.parseDouble(amountReceivedField.getText());
                change = received - amountDue;
                if (received < amountDue) {
                    showError("Insufficient amount received.", "Please enter at least $" + String.format("%.2f", amountDue));
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Invalid amount", "Please enter a valid numeric amount.");
                return;
            }
        }

        result = new PaymentResult(method, amountDue, received, change);
        close();
    }

    private void handleCancel() {
        result = null;
        close();
    }

    private void close() {
        Stage stage = (Stage) confirmButton.getScene().getWindow();
        stage.close();
    }

    public PaymentResult getResult() {
        return result;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        UiEffects.styleDialog(alert);
        alert.showAndWait();

        if (amountReceivedField != null) {
            amountReceivedField.requestFocus();
            amountReceivedField.selectAll();
        }
    }

    public static class PaymentResult {
        private final String method;
        private final double amountDue;
        private final double received;
        private final double change;

        public PaymentResult(String method, double amountDue, double received, double change) {
            this.method = method;
            this.amountDue = amountDue;
            this.received = received;
            this.change = change;
        }

        public String getMethod() { return method; }
        public double getAmountDue() { return amountDue; }
        public double getReceived() { return received; }
        public double getChange() { return change; }
    }
}
