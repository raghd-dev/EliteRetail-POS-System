package com.eliteretail.pos.controller;

import com.eliteretail.pos.model.Discount;
import com.eliteretail.pos.service.DiscountService;
import com.eliteretail.pos.utils.UiEffects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class DiscountManagementController implements Initializable {

    @FXML private ListView<Discount> discountList;
    @FXML private Button addButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button backButton;
    @FXML private ComboBox<String> filterCombo;
    @FXML private TextField searchField;
    @FXML private Label statusLabel;

    private final DiscountService discountService = new DiscountService();
    private final ObservableList<Discount> discounts = FXCollections.observableArrayList();
    private final ObservableList<String> productNames = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDiscounts();
        loadProductNames();
        setupListView();
        setupButtonActions();
        setupFilters();
    }

    private void loadProductNames() {
        List<String> names = discountService.getAllProductNames();
        productNames.setAll(names);
    }

    private void loadDiscounts() {
        discounts.setAll(discountService.getAllDiscounts());
    }

    private void setupListView() {
        discountList.setItems(discounts);
        discountList.setCellFactory(lv -> new ListCell<Discount>() {
            @Override
            protected void updateItem(Discount discount, boolean empty) {
                super.updateItem(discount, empty);
                if (empty || discount == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(discount.toString());
                    if (discount.isValid()) {
                        setStyle("-fx-text-fill: #2F855A; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #8795A7;");
                    }
                }
            }
        });
    }

    private void setupButtonActions() {
        addButton.setOnAction(e -> handleAddDiscount());
        editButton.setOnAction(e -> handleEditDiscount());
        deleteButton.setOnAction(e -> handleDeleteDiscount());
        backButton.setOnAction(e -> handleBack());
    }

    private void setupFilters() {
        filterCombo.getItems().addAll("All Discounts", "Active Only", "Expired");
        filterCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal;  // Get the new filter value
            String searchText = searchField.getText().toLowerCase();
            applyFilters(filter, searchText);  // Pass both filter and search text
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = filterCombo.getSelectionModel().getSelectedItem();
            applyFilters(filter, newVal.toLowerCase());  // Pass both filter and updated search text
        });

        filterCombo.getSelectionModel().selectFirst();
        updateStatusLabel();
    }

    private void applyFilters(String filter, String searchText) {
        ObservableList<Discount> filtered = FXCollections.observableArrayList();

        for (Discount discount : discountService.getAllDiscounts()) {
            boolean matchesFilter = true;
            boolean matchesSearch = searchText.isEmpty() ||
                    discount.getName().toLowerCase().contains(searchText);

            if ("Active Only".equals(filter)) {
                matchesFilter = discount.isValid();
            } else if ("Expired".equals(filter)) {
                matchesFilter = !discount.isValid();
            }

            if (matchesFilter && matchesSearch) {
                filtered.add(discount);
            }
        }

        discounts.setAll(filtered);
        updateStatusLabel();
    }

    private void applyCurrentFilters() {
        String filter = filterCombo.getSelectionModel().getSelectedItem();
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
        applyFilters(filter, searchText);
    }

    private void updateStatusLabel() {
        int total = discounts.size();
        long activeCount = discounts.stream().filter(Discount::isValid).count();
        statusLabel.setText(String.format("Showing %d discounts (%d active)", total, activeCount));
    }


    private void handleAddDiscount() {
        Discount newDiscount = showDiscountDialog(null);
        if (newDiscount != null) {
            try {
                if (discountService.addDiscount(newDiscount)) {
                    loadDiscounts();
                    applyCurrentFilters();
                    showAlert("Success", "Discount added successfully!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to add discount.", Alert.AlertType.ERROR);
                }
            } catch (IllegalArgumentException e) {
                showAlert("Validation Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleEditDiscount() {
        Discount selected = discountList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a discount to edit.", Alert.AlertType.WARNING);
            return;
        }

        Discount updatedDiscount = showDiscountDialog(selected);
        if (updatedDiscount != null) {
            try {
                if (discountService.updateDiscount(updatedDiscount)) {
                    loadDiscounts();
                    applyCurrentFilters();
                    showAlert("Success", "Discount updated successfully!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to update discount.", Alert.AlertType.ERROR);
                }
            } catch (IllegalArgumentException e) {
                showAlert("Validation Error", e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void handleDeleteDiscount() {
        Discount selected = discountList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Selection Required", "Please select a discount to delete.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Discount");
        confirm.setContentText("Are you sure you want to delete: " + selected.getName() + "?");
        UiEffects.styleDialog(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (discountService.deleteDiscount(selected.getId())) {
                discounts.remove(selected);
                showAlert("Success", "Discount deleted successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to delete discount.", Alert.AlertType.ERROR);
            }
        }
    }

    private Discount showDiscountDialog(Discount existingDiscount) {
        Dialog<Discount> dialog = new Dialog<>();
        dialog.setTitle(existingDiscount == null ? "Add Discount" : "Edit Discount");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        UiEffects.styleDialog(dialog);

        TextField nameField = new TextField();
        ComboBox<String> typeCombo = new ComboBox<>();
        TextField valueField = new TextField();
        TextField targetValueField = new TextField();
        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();
        TextField usageLimitField = new TextField();
        CheckBox activeCheckbox = new CheckBox("Active");

        ComboBox<String> applyToCombo = new ComboBox<>();
        applyToCombo.setPromptText("Select Product");
        applyToCombo.setItems(productNames);
        applyToCombo.getItems().add(0, "ALL"); // Add "ALL" option at the top

        typeCombo.getItems().addAll("PERCENT", "FIXED_AMOUNT", "BUY_X_GET_Y");
        typeCombo.setValue("PERCENT");

        usageLimitField.setText("0");
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(1));
        activeCheckbox.setSelected(true);
        targetValueField.setPromptText("e.g., 2+1");

        if (existingDiscount != null) {
            nameField.setText(existingDiscount.getName());
            typeCombo.setValue(existingDiscount.getType());
            valueField.setText(String.valueOf(existingDiscount.getValue()));
            startDatePicker.setValue(existingDiscount.getStartDate());
            endDatePicker.setValue(existingDiscount.getEndDate());
            usageLimitField.setText(String.valueOf(existingDiscount.getUsageLimit()));
            activeCheckbox.setSelected(existingDiscount.isActive());
            targetValueField.setText(existingDiscount.getTargetValue());

            if (existingDiscount.getApplyTo() != null && !existingDiscount.getApplyTo().isEmpty()) {
                applyToCombo.setValue(existingDiscount.getApplyTo());
            } else {
                applyToCombo.setValue("ALL");
            }
        } else {
            applyToCombo.setValue("ALL");
        }

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));
        grid.setPrefWidth(480);

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Value:"), 0, 2);
        grid.add(valueField, 1, 2);

        Label targetValueLabel = new Label("Target Value (X+Y):");
        grid.add(targetValueLabel, 0, 3);
        grid.add(targetValueField, 1, 3);
        targetValueLabel.setVisible(false);
        targetValueField.setVisible(false);

        grid.add(new Label("Apply to Product:"), 0, 4);
        grid.add(applyToCombo, 1, 4);
        grid.add(new Label("Start Date:"), 0, 5);
        grid.add(startDatePicker, 1, 5);
        grid.add(new Label("End Date:"), 0, 6);
        grid.add(endDatePicker, 1, 6);
        grid.add(new Label("Usage Limit (0=unlimited):"), 0, 7);
        grid.add(usageLimitField, 1, 7);
        grid.add(new Label("Active:"), 0, 8);
        grid.add(activeCheckbox, 1, 8);

        typeCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                updateDiscountTypeFields(newVal, targetValueLabel, targetValueField, valueField));

        updateDiscountTypeFields(typeCombo.getValue(), targetValueLabel, targetValueField, valueField);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    String name = nameField.getText();
                    String type = typeCombo.getValue();
                    double value = Double.parseDouble(valueField.getText());
                    LocalDate startDate = startDatePicker.getValue();
                    LocalDate endDate = endDatePicker.getValue();
                    int usageLimit = Integer.parseInt(usageLimitField.getText());
                    boolean active = activeCheckbox.isSelected();
                    String applyTo = applyToCombo.getValue() != null ? applyToCombo.getValue() : "ALL";
                    String targetValue = targetValueField.getText().trim();

                    if ("BUY_X_GET_Y".equals(type)) {
                        if (!DiscountService.isValidBuyXGetYTarget(targetValue)) {
                            showAlert("Input Error", "Please enter valid target value in format X+Y (e.g., 2+1)", Alert.AlertType.ERROR);
                            return null;
                        }
                    }

                    if (existingDiscount != null) {
                        return new Discount(
                                existingDiscount.getId(),
                                name,
                                existingDiscount.getDescription(),
                                value,
                                type,
                                applyTo,
                                targetValue,
                                startDate,
                                endDate,
                                existingDiscount.getMinQuantity(),
                                existingDiscount.getMinPurchaseAmount(),
                                active,
                                usageLimit,
                                existingDiscount.getTimesUsed()
                        );
                    }

                    Discount discount = new Discount(name, type, value, active, startDate, endDate, usageLimit, applyTo);
                    discount.setTargetValue(targetValue);
                    return discount;
                } catch (NumberFormatException e) {
                    showAlert("Input Error", "Please enter valid numbers for value and usage limit.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        Optional<Discount> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void updateDiscountTypeFields(String type, Label targetLabel, TextField targetField, TextField valueField) {
        boolean buyXGetY = "BUY_X_GET_Y".equals(type);
        targetLabel.setVisible(buyXGetY);
        targetLabel.setManaged(buyXGetY);
        targetField.setVisible(buyXGetY);
        targetField.setManaged(buyXGetY);
        valueField.setDisable(buyXGetY);
        if (buyXGetY) {
            valueField.setText("0");
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase();
        String filter = filterCombo.getSelectionModel().getSelectedItem();
        applyFilters(filter, query);
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initDashboard();
            UiEffects.prepare(root);

            Stage stage = (Stage) backButton.getScene().getWindow();

            UiEffects.applyStageBranding(stage);
            stage.setScene(new Scene(root, 1180, 760));
            stage.setTitle("EliteRetail POS - Dashboard");
            stage.show();

        } catch (Exception e) {
            showAlert("Error", "Cannot return to dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        UiEffects.styleDialog(alert);
        alert.showAndWait();
    }
}
