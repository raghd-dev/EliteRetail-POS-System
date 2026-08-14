package com.eliteretail.pos.controller;

import com.eliteretail.pos.model.ProductItem;
import com.eliteretail.pos.model.RecentTransaction;
import com.eliteretail.pos.service.AuthService;
import com.eliteretail.pos.service.ProductService;
import com.eliteretail.pos.service.StatisticsService;
import com.eliteretail.pos.utils.UiEffects;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label dashboardDateLabel;
    @FXML private Label revenueTodayLabel;
    @FXML private Label transactionsTodayLabel;
    @FXML private Label totalProductsLabel;
    @FXML private Label lowStockLabel;
    @FXML private BarChart<String, Number> dashboardSalesChart;
    @FXML private CategoryAxis dashboardXAxis;
    @FXML private NumberAxis dashboardYAxis;
    @FXML private ListView<String> recentTransactionsList;
    @FXML private Button manageProductsButton;
    @FXML private Button manageDiscountsButton;
    @FXML private Button viewStatisticsButton;
    @FXML private Button processSalesButton;

    private final AuthService authService = AuthService.getInstance();
    private final StatisticsService statisticsService = new StatisticsService();
    private final ProductService productService = new ProductService();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
    private final DateTimeFormatter dashboardDate = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US);
    private final DateTimeFormatter transactionTime = DateTimeFormatter.ofPattern("HH:mm");

    public void initDashboard() {
        dashboardDateLabel.setText(LocalDate.now().format(dashboardDate));
        if (authService.getCurrentUser() == null) {
            setWelcomeMessage("Welcome");
            roleLabel.setText("SIGNED OUT");
            disableAllActions();
            return;
        }

        String displayName = authService.getCurrentCashierName().isBlank()
                ? authService.getCurrentCashierCode()
                : authService.getCurrentCashierName();
        welcomeLabel.setText(displayName);
        roleLabel.setText(authService.getCurrentUser().getRole().name());
        refreshButtons();
        if (authService.canViewReports()) {
            loadOperationalOverview();
        } else {
            loadCashierOverview();
        }
    }

    public void refreshButtons() {
        manageProductsButton.setDisable(!authService.canManageProducts());
        manageDiscountsButton.setDisable(!authService.canManageDiscounts());
        viewStatisticsButton.setDisable(!authService.canViewReports());
        processSalesButton.setDisable(!authService.canProcessSales());
    }


    private void loadCashierOverview() {
        revenueTodayLabel.setText("Restricted");
        transactionsTodayLabel.setText("Restricted");
        dashboardSalesChart.getData().clear();
        recentTransactionsList.getItems().setAll("Manager access is required for sales analytics.");

        Thread worker = new Thread(() -> {
            List<ProductItem> products = productService.getAllProducts();
            long lowStock = products.stream().filter(product -> product.getStockQuantity() <= 5).count();
            Platform.runLater(() -> {
                totalProductsLabel.setText(String.valueOf(products.size()));
                lowStockLabel.setText(String.valueOf(lowStock));
            });
        }, "dashboard-catalog-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadOperationalOverview() {
        Thread worker = new Thread(() -> {
            double revenue = statisticsService.getTotalSalesToday();
            int transactions = statisticsService.getTotalTransactionsToday();
            Map<String, Double> hourlySales = statisticsService.getSalesByHour();
            List<RecentTransaction> recent = statisticsService.getRecentTransactions(6);
            List<ProductItem> products = productService.getAllProducts();
            long lowStock = products.stream().filter(product -> product.getStockQuantity() <= 5).count();

            Platform.runLater(() -> {
                revenueTodayLabel.setText(currency.format(revenue));
                transactionsTodayLabel.setText(String.valueOf(transactions));
                totalProductsLabel.setText(String.valueOf(products.size()));
                lowStockLabel.setText(String.valueOf(lowStock));
                updateSalesChart(hourlySales);
                updateRecentTransactions(recent);
            });
        }, "dashboard-overview-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateSalesChart(Map<String, Double> hourlySales) {
        dashboardXAxis.setLabel("Hour");
        dashboardYAxis.setLabel("Sales ($)");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        hourlySales.forEach((hour, amount) -> series.getData().add(new XYChart.Data<>(hour, amount)));
        dashboardSalesChart.getData().setAll(series);
    }

    private void updateRecentTransactions(List<RecentTransaction> recent) {
        recentTransactionsList.getItems().clear();
        for (RecentTransaction transaction : recent) {
            String time = transaction.createdAt() == null ? "—" : transaction.createdAt().format(transactionTime);
            recentTransactionsList.getItems().add(String.format(
                    Locale.US,
                    "%s   •   %s   •   %s   •   %s",
                    transaction.billNumber(),
                    currency.format(transaction.total()),
                    transaction.paymentMethod().replace('_', ' '),
                    time
            ));
        }
        if (recentTransactionsList.getItems().isEmpty()) {
            recentTransactionsList.getItems().add("No completed transactions yet today.");
        }
    }

    private void disableAllActions() {
        manageProductsButton.setDisable(true);
        manageDiscountsButton.setDisable(true);
        viewStatisticsButton.setDisable(true);
        processSalesButton.setDisable(true);
    }

    @FXML
    private void handleManageProducts(ActionEvent event) {
        if (authService.canManageProducts()) {
            switchScene(event, "/fxml/product-management.fxml", "EliteRetail POS - Product Management");
        }
    }

    @FXML
    private void handleManageDiscounts(ActionEvent event) {
        if (authService.canManageDiscounts()) {
            switchScene(event, "/fxml/discount-management.fxml", "EliteRetail POS - Discount Management");
        }
    }

    @FXML
    private void handleViewStatistics(ActionEvent event) {
        if (authService.canViewReports()) {
            switchScene(event, "/fxml/statistics.fxml", "EliteRetail POS - Statistics");
        }
    }

    @FXML
    private void handleProcessSales(ActionEvent event) {
        if (!authService.canProcessSales()) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/sales.fxml"));
            Parent root = loader.load();
            SalesController controller = loader.getController();
            controller.setCashierInfo(authService.getCurrentCashierName(), authService.getCurrentCashierCode());
            UiEffects.prepare(root);

            Stage stage = currentStage(event);
            UiEffects.applyStageBranding(stage);
            stage.setTitle("EliteRetail POS - Sales Register");
            stage.setScene(new Scene(root, 1240, 800));
            stage.show();
        } catch (Exception e) {
            showNavigationError("Could not open the sales screen: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        authService.logout();
        switchScene(event, "/fxml/login.fxml", "EliteRetail POS System");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            UiEffects.prepare(root);
            Stage stage = currentStage(event);
            UiEffects.applyStageBranding(stage);
            stage.setTitle(title);
            stage.setScene(new Scene(root, 1180, 760));
            stage.show();
        } catch (Exception e) {
            showNavigationError("Could not load screen: " + e.getMessage());
        }
    }

    private Stage currentStage(ActionEvent event) {
        return (Stage) ((Node) event.getSource()).getScene().getWindow();
    }

    private void showNavigationError(String message) {
        welcomeLabel.setText(message);
    }

    public void setWelcomeMessage(String message) {
        if (welcomeLabel != null) {
            welcomeLabel.setText(message);
        }
    }
}
