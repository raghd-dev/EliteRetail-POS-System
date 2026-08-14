package com.eliteretail.pos.controller;

import com.eliteretail.pos.service.AuthService;
import com.eliteretail.pos.utils.UiEffects;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.prefs.Preferences;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private CheckBox rememberMeCheckBox;

    private static final String REMEMBERED_CASHIER_KEY = "rememberedCashierCode";
    private final Preferences preferences = Preferences.userNodeForPackage(LoginController.class);
    private final AuthService authService = AuthService.getInstance();

    @FXML
    public void initialize() {
        String rememberedCashier = preferences.get(REMEMBERED_CASHIER_KEY, "");
        if (!rememberedCashier.isBlank()) {
            usernameField.setText(rememberedCashier);
            rememberMeCheckBox.setSelected(true);
        }
    }

    @FXML
    protected void handleLogin() {
        String cashierCode = usernameField.getText().trim();
        String password = passwordField.getText();

        if (cashierCode.isEmpty() || password.isBlank()) {
            showMessage("Please enter both code and password.");
            return;
        }

        if (authService.login(cashierCode, password)) {
            if (rememberMeCheckBox.isSelected()) {
                preferences.put(REMEMBERED_CASHIER_KEY, cashierCode);
            } else {
                preferences.remove(REMEMBERED_CASHIER_KEY);
            }
            passwordField.clear();
            openDashboard();
        } else {
            passwordField.clear();
            showMessage("Invalid cashier code or password.");
        }
    }

    private void openDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();
            DashboardController controller = loader.getController();
            controller.initDashboard();
            UiEffects.prepare(root);

            Stage stage = (Stage) usernameField.getScene().getWindow();
            UiEffects.applyStageBranding(stage);
            stage.setTitle("EliteRetail POS - Dashboard");
            stage.setScene(new Scene(root, 1180, 760));
            stage.show();
        } catch (Exception e) {
            showMessage("Could not open the dashboard: " + e.getMessage());
        }
    }

    @FXML
    protected void handleCancel() {
        usernameField.clear();
        passwordField.clear();
        ((Stage) usernameField.getScene().getWindow()).close();
    }

    private void showMessage(String message) {
        messageLabel.setText(message);
    }
}
