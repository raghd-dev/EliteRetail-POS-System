package com.eliteretail.pos.controller;

import com.eliteretail.pos.service.ProductService;
import com.eliteretail.pos.model.ProductCategory;
import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.utils.UiEffects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.util.StringConverter;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class ProductManagementController {

    @FXML private TableView<ProductItem> productsTable;
    @FXML private TableColumn<ProductItem, String> codeColumn;
    @FXML private TableColumn<ProductItem, String> nameColumn;
    @FXML private TableColumn<ProductItem, String> descriptionColumn;
    @FXML private TableColumn<ProductItem, Double> priceColumn;
    @FXML private TableColumn<ProductItem, Integer> stockColumn;
    @FXML private TableColumn<ProductItem, String> categoryColumn;
    @FXML private TextField searchField;

    private final ProductService productService = new ProductService();
    private ObservableList<ProductItem> productsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadProducts();
    }

    private void setupTableColumns() {
        codeColumn.setCellValueFactory(new PropertyValueFactory<>("code"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        stockColumn.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        priceColumn.setCellFactory(column -> new TableCell<ProductItem, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("$%.2f", price));
            }
        });

        TableColumn<ProductItem, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(150);
        actionsCol.setCellFactory(param -> new TableCell<ProductItem, Void>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.getStyleClass().add("secondary-button");
                deleteBtn.getStyleClass().add("danger-button");
                editBtn.setOnAction(event -> {
                    ProductItem product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });
                deleteBtn.setOnAction(event -> {
                    ProductItem product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, deleteBtn));
            }
        });

        productsTable.getColumns().add(actionsCol);
    }

    private void loadProducts() {
        try {
            productsList.setAll(productService.getAllProducts());
            productsTable.setItems(productsList);
        } catch (Exception e) {
            showAlert("Error", "Failed to load products: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();
        if (searchTerm.isEmpty()) {
            productsTable.setItems(productsList);
            return;
        }

        ObservableList<ProductItem> filteredList = FXCollections.observableArrayList();
        for (ProductItem product : productsList) {
            if (product.getCode().toLowerCase().contains(searchTerm) ||
                    product.getName().toLowerCase().contains(searchTerm) ||
                    product.getCategory().toLowerCase().contains(searchTerm)) {
                filteredList.add(product);
            }
        }
        productsTable.setItems(filteredList);
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        productsTable.setItems(productsList);
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    @FXML
    private void handleAddCategory() {
        showAddCategoryDialog();
    }

    @FXML
    private void handleEditCategories() {
        showEditCategoriesDialog();
    }

    private void handleEditProduct(ProductItem product) {
        showProductDialog(product);
    }

    private void handleDeleteProduct(ProductItem product) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Product");
        confirm.setContentText("Are you sure you want to delete: " + product.getName() + "?");
        UiEffects.styleDialog(confirm);

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    if (productService.deleteProduct(product.getCode())) {
                        showAlert("Success", "Product deleted successfully!", Alert.AlertType.INFORMATION);
                        loadProducts();
                    } else {
                        showAlert("Error", "Failed to delete product.", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    showAlert("Error", "Error deleting product: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showProductDialog(ProductItem existingProduct) {
        try {
            Dialog<ProductItem> dialog = new Dialog<>();
            dialog.setTitle(existingProduct == null ? "Add New Product" : "Edit Product");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            UiEffects.styleDialog(dialog);

            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(12);
            grid.setPadding(new Insets(8));
            grid.setPrefWidth(480);

            ComboBox<Object> categoryCombo = new ComboBox<>();
            TextField codeField = new TextField();
            TextField nameField = new TextField();
            TextField descriptionField = new TextField();
            TextField priceField = new TextField();
            TextField stockField = new TextField();

            categoryCombo.getItems().addAll(ProductCategory.getAllCategories());
            categoryCombo.setConverter(new StringConverter<Object>() {
                @Override
                public String toString(Object category) {
                    if (category instanceof ProductCategory) {
                        return ((ProductCategory) category).getDisplayName();
                    } else if (category instanceof ProductCategory.CustomCategory) {
                        return ((ProductCategory.CustomCategory) category).getDisplayName();
                    }
                    return "";
                }

                @Override
                public Object fromString(String string) {
                    return categoryCombo.getItems().stream()
                            .filter(item -> toString(item).equals(string))
                            .findFirst()
                            .orElse(null);
                }
            });

            categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && (existingProduct == null || codeField.getText().isEmpty())) {
                    String prefix = getPrefixFromCategory(newVal);
                    if (prefix != null) {
                        codeField.setText(generateProductCode(prefix));
                    }
                }
            });

            if (existingProduct != null) {
                Object existingCategory = ProductCategory.fromCode(existingProduct.getCode());
                if (existingCategory != null) {
                    categoryCombo.setValue(existingCategory);
                }
                codeField.setText(existingProduct.getCode());
                codeField.setDisable(true);
                nameField.setText(existingProduct.getName());
                descriptionField.setText(existingProduct.getDescription());
                priceField.setText(String.valueOf(existingProduct.getUnitPrice()));
                stockField.setText(String.valueOf(existingProduct.getStockQuantity()));
            }

            grid.add(new Label("Category:*"), 0, 0);
            grid.add(categoryCombo, 1, 0);
            grid.add(new Label("Code:*"), 0, 1);
            grid.add(codeField, 1, 1);
            grid.add(new Label("Name:*"), 0, 2);
            grid.add(nameField, 1, 2);
            grid.add(new Label("Description:"), 0, 3);
            grid.add(descriptionField, 1, 3);
            grid.add(new Label("Price:*"), 0, 4);
            grid.add(priceField, 1, 4);
            grid.add(new Label("Stock:*"), 0, 5);
            grid.add(stockField, 1, 5);

            Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);

            List<TextField> requiredFields = Arrays.asList(codeField, nameField, priceField, stockField);
            for (TextField field : requiredFields) {
                field.textProperty().addListener((obs, oldVal, newVal) ->
                        validateForm(saveButton, categoryCombo, requiredFields));
            }
            categoryCombo.valueProperty().addListener((obs, oldVal, newVal) ->
                    validateForm(saveButton, categoryCombo, requiredFields));

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        Object selectedCategory = categoryCombo.getValue();
                        if (selectedCategory == null) {
                            showAlert("Error", "Please select a category", Alert.AlertType.ERROR);
                            return null;
                        }

                        String code = codeField.getText();
                        String expectedPrefix = getPrefixFromCategory(selectedCategory);
                        if (expectedPrefix != null && !code.startsWith(expectedPrefix)) {
                            showAlert("Error", "Code must start with " + expectedPrefix, Alert.AlertType.ERROR);
                            return null;
                        }

                        String categoryName = getDisplayNameFromCategory(selectedCategory);
                        return new ProductItem(
                                code, nameField.getText(), descriptionField.getText(),
                                Double.parseDouble(priceField.getText()),
                                Integer.parseInt(stockField.getText()),
                                categoryName
                        );
                    } catch (NumberFormatException e) {
                        showAlert("Error", "Please enter valid numbers", Alert.AlertType.ERROR);
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(resultProduct -> {
                try {
                    boolean success = existingProduct == null ?
                            productService.addProduct(resultProduct) :
                            productService.updateProduct(resultProduct);

                    if (success) {
                        showAlert("Success", "Product saved successfully!", Alert.AlertType.INFORMATION);
                        loadProducts();
                    } else {
                        showAlert("Error", "Failed to save product", Alert.AlertType.ERROR);
                    }
                } catch (Exception e) {
                    showAlert("Error", "Error saving product: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });

        } catch (Exception e) {
            showAlert("Error", "Error opening dialog: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAddCategoryDialog() {
        Dialog<CategoryInput> dialog = new Dialog<>();
        dialog.setTitle("Add New Category");
        dialog.setHeaderText("Create a new product category");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        UiEffects.styleDialog(dialog);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(8));
        grid.setPrefWidth(480);

        TextField nameField = new TextField();
        nameField.setPromptText("Category Name");
        TextField prefixField = new TextField();
        prefixField.setPromptText("Code Prefix (4 digits)");
        prefixField.setText(ProductCategory.getNextAvailablePrefix());

        grid.add(new Label("Category Name:*"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Code Prefix:*"), 0, 1);
        grid.add(prefixField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        nameField.textProperty().addListener((obs, oldVal, newVal) ->
                validateCategoryForm(createButton, nameField, prefixField));
        prefixField.textProperty().addListener((obs, oldVal, newVal) ->
                validateCategoryForm(createButton, nameField, prefixField));

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return new CategoryInput(nameField.getText(), prefixField.getText());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            try {
                if (!result.prefix.matches("\\d{4}")) {
                    showAlert("Error", "Prefix must be 4 digits", Alert.AlertType.ERROR);
                    return;
                }

                for (Object category : ProductCategory.getAllCategories()) {
                    String existingPrefix = getPrefixFromCategory(category);
                    if (existingPrefix != null && existingPrefix.equals(result.prefix)) {
                        showAlert("Error", "Prefix already exists", Alert.AlertType.ERROR);
                        return;
                    }
                }

                ProductCategory.addCustomCategory(result.name, result.prefix);
                showAlert("Success", "Category added successfully!", Alert.AlertType.INFORMATION);

            } catch (Exception e) {
                showAlert("Error", "Failed to add category: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void showEditCategoriesDialog() {
        try {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Edit Categories");
            dialog.setHeaderText("Manage existing product categories");

            ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
            UiEffects.styleDialog(dialog);

            List<CategoryEditItem> editItems = new ArrayList<>();
            GridPane grid = new GridPane();
            grid.setHgap(16);
            grid.setVgap(12);
            grid.setPadding(new Insets(8));
            grid.setPrefWidth(480);

            grid.add(new Label("Category Name"), 0, 0);
            grid.add(new Label("Code Prefix"), 1, 0);
            grid.add(new Label("Actions"), 2, 0);

            List<Object> categories = new ArrayList<>(List.of(ProductCategory.getAllCategories()));
            for (int i = 0; i < categories.size(); i++) {
                Object category = categories.get(i);
                String currentName = getDisplayNameFromCategory(category);
                String currentPrefix = getPrefixFromCategory(category);

                TextField nameField = new TextField(currentName);
                TextField prefixField = new TextField(currentPrefix);
                prefixField.setDisable(true);

                Button deleteButton = new Button("Delete");
                deleteButton.getStyleClass().add("danger-button");
                deleteButton.setDisable(isBuiltInCategory(category));

                int row = i + 1;
                grid.add(nameField, 0, row);
                grid.add(prefixField, 1, row);
                grid.add(deleteButton, 2, row);

                editItems.add(new CategoryEditItem(category, nameField, prefixField, deleteButton));
            }

            for (CategoryEditItem item : editItems) {
                item.deleteButton.setOnAction(event -> {
                    if (!isBuiltInCategory(item.category)) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Confirm Delete");
                        confirm.setHeaderText("Delete Category");
                        confirm.setContentText("Are you sure you want to delete this category? Products in this category will need to be reassigned.");
                        UiEffects.styleDialog(confirm);

                        confirm.showAndWait().ifPresent(response -> {
                            if (response == ButtonType.OK) {
                                try {
                                    ProductCategory.removeCustomCategory(item.category);
                                    showAlert("Success", "Category deleted successfully!", Alert.AlertType.INFORMATION);
                                    dialog.close();
                                    showEditCategoriesDialog();
                                } catch (Exception e) {
                                    showAlert("Error", "Failed to delete category: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                            }
                        });
                    }
                });
            }

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        for (CategoryEditItem item : editItems) {
                            String newName = item.nameField.getText().trim();
                            if (!newName.isEmpty() && !newName.equals(getDisplayNameFromCategory(item.category))) {
                                if (!isBuiltInCategory(item.category)) {
                                    ProductCategory.renameCustomCategory(item.category, newName);
                                }
                            }
                        }
                        showAlert("Success", "Category changes saved successfully!", Alert.AlertType.INFORMATION);
                    } catch (Exception e) {
                        showAlert("Error", "Failed to save category changes: " + e.getMessage(), Alert.AlertType.ERROR);
                    }
                }
                return null;
            });

            dialog.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Error opening category editor: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private boolean isBuiltInCategory(Object category) {
        return category instanceof ProductCategory;
    }

    private String generateProductCode(String prefix) {
        try {
            int maxSequence = 0;
            for (ProductItem product : productService.getAllProducts()) {
                if (product.getCode().startsWith(prefix)) {
                    try {
                        int sequence = Integer.parseInt(product.getCode().substring(4)); // FIXED: sequenhelloce → sequence
                        maxSequence = Math.max(maxSequence, sequence);
                    } catch (NumberFormatException ignored) {}
                }
            }
            return prefix + String.format("%04d", maxSequence + 1);
        } catch (Exception e) {
            return prefix + "0001";
        }
    }

    private String getPrefixFromCategory(Object category) {
        if (category instanceof ProductCategory) {
            return ((ProductCategory) category).getCodePrefix();
        } else if (category instanceof ProductCategory.CustomCategory) {
            return ((ProductCategory.CustomCategory) category).getCodePrefix();
        }
        return null;
    }

    private String getDisplayNameFromCategory(Object category) {
        if (category instanceof ProductCategory) {
            return ((ProductCategory) category).getDisplayName();
        } else if (category instanceof ProductCategory.CustomCategory) {
            return ((ProductCategory.CustomCategory) category).getDisplayName();
        }
        return null;
    }

    private void validateForm(Node saveButton, ComboBox<Object> categoryCombo, List<TextField> requiredFields) {
        boolean isValid = categoryCombo.getValue() != null;
        for (TextField field : requiredFields) {
            if (field.getText() == null || field.getText().trim().isEmpty()) {
                isValid = false;
                break;
            }
        }
        if (isValid) {
            String code = requiredFields.get(0).getText();
            String expectedPrefix = getPrefixFromCategory(categoryCombo.getValue());
            isValid = expectedPrefix != null && code.startsWith(expectedPrefix);
        }
        saveButton.setDisable(!isValid);
    }

    private void validateCategoryForm(Node createButton, TextField nameField, TextField prefixField) {
        boolean isValid = !nameField.getText().trim().isEmpty() &&
                !prefixField.getText().trim().isEmpty() &&
                prefixField.getText().matches("\\d{4}");
        createButton.setDisable(!isValid);
    }

    @FXML
    private void handleBackToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initDashboard();
            UiEffects.prepare(root);

            Stage stage = (Stage) productsTable.getScene().getWindow();

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

    private static class CategoryInput {
        public final String name;
        public final String prefix;

        public CategoryInput(String name, String prefix) {
            this.name = name;
            this.prefix = prefix;
        }
    }

    private static class CategoryEditItem {
        public final Object category;
        public final TextField nameField;
        public final TextField prefixField;
        public final Button deleteButton;

        public CategoryEditItem(Object category, TextField nameField, TextField prefixField, Button deleteButton) {
            this.category = category;
            this.nameField = nameField;
            this.prefixField = prefixField;
            this.deleteButton = deleteButton;
        }
    }
}
