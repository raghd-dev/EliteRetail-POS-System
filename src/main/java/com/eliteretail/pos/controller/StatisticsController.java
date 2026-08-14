package com.eliteretail.pos.controller;

import com.eliteretail.pos.model.ProductStats;
import com.eliteretail.pos.service.StatisticsService;
import com.eliteretail.pos.utils.ReportExporter;
import com.eliteretail.pos.utils.UiEffects;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

public class StatisticsController {
    @FXML private Label totalSalesLabel;
    @FXML private Label totalTransactionsLabel;
    @FXML private Label averageTransactionLabel;
    @FXML private Label totalDiscountsLabel;
    @FXML private BarChart<String, Number> salesChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private TableView<ProductSales> topProductsTable;
    @FXML private TableColumn<ProductSales, String> productNameColumn;
    @FXML private TableColumn<ProductSales, Number> salesAmountColumn;
    @FXML private TableColumn<ProductSales, Number> quantityColumn;

    private final StatisticsService statisticsService = new StatisticsService();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    private void initialize() {
        xAxis.setLabel("Hour of Day");
        yAxis.setLabel("Sales Amount ($)");
        salesChart.setLegendVisible(false);

        productNameColumn.setCellValueFactory(new PropertyValueFactory<>("productName"));
        salesAmountColumn.setCellValueFactory(new PropertyValueFactory<>("salesAmount"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantitySold"));
        refreshData();
    }

    @FXML
    private void handleRefresh() {
        refreshData();
    }

    @FXML
    private void onExportReportClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Daily Statistics");
        chooser.setInitialFileName("daily_report.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(totalSalesLabel.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            ReportExporter.exportTodayReportToCsv(file.toPath());
            showAlert("Export Complete", "Report saved to " + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Export Failed", e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClose() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initDashboard();
            UiEffects.prepare(root);

            Stage stage = (Stage) totalSalesLabel.getScene().getWindow();
            UiEffects.applyStageBranding(stage);
            stage.setScene(new Scene(root, 1180, 760));
            stage.setTitle("EliteRetail POS - Dashboard");
            stage.show();
        } catch (Exception e) {
            showAlert("Navigation Error", "Cannot return to dashboard: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void refreshData() {
        Thread worker = new Thread(() -> {
            double totalSales = statisticsService.getTotalSalesToday();
            int totalTransactions = statisticsService.getTotalTransactionsToday();
            double avgTransaction = statisticsService.getAverageTransactionValue();
            double totalDiscounts = statisticsService.getTotalDiscountsToday();
            Map<String, Double> salesByHour = statisticsService.getSalesByHour();
            Map<String, ProductStats> topProducts = statisticsService.getTopSellingProductsDetailed(5);

            Platform.runLater(() -> updateUi(
                    totalSales, totalTransactions, avgTransaction, totalDiscounts, salesByHour, topProducts
            ));
        }, "statistics-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateUi(
            double totalSales,
            int totalTransactions,
            double avgTransaction,
            double totalDiscounts,
            Map<String, Double> salesByHour,
            Map<String, ProductStats> topProducts
    ) {
        totalSalesLabel.setText(currencyFormat.format(totalSales));
        totalTransactionsLabel.setText(String.valueOf(totalTransactions));
        averageTransactionLabel.setText(currencyFormat.format(avgTransaction));
        totalDiscountsLabel.setText(currencyFormat.format(totalDiscounts));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        salesByHour.forEach((hour, sales) -> series.getData().add(new XYChart.Data<>(hour, sales)));
        salesChart.getData().setAll(series);

        ObservableList<ProductSales> rows = FXCollections.observableArrayList();
        topProducts.forEach((name, stats) ->
                rows.add(new ProductSales(name, stats.getSalesAmount(), stats.getQuantitySold()))
        );
        topProductsTable.setItems(rows);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        UiEffects.styleDialog(alert);
        alert.showAndWait();
    }

    public static class ProductSales {
        private final String productName;
        private final Number salesAmount;
        private final Number quantitySold;

        public ProductSales(String productName, Number salesAmount, Number quantitySold) {
            this.productName = productName;
            this.salesAmount = salesAmount;
            this.quantitySold = quantitySold;
        }

        public String getProductName() { return productName; }
        public Number getSalesAmount() { return salesAmount; }
        public Number getQuantitySold() { return quantitySold; }
    }
}
