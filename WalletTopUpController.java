package controller;

import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import service.CartService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class WalletTopUpController {
    @FXML private TextField amountField;
    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;
    @FXML private Button topUpButton;
    @FXML private Label balanceLabel;
    private final CartService cartService = new CartService();
    private final Gson gson = new Gson();
    private String token;
    private Stage stage;
    private Timeline balanceUpdateTimeline;
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
    }
    public void setBankDetails(String bankName, String accountNumber) {
        if (bankName != null) bankNameField.setText(bankName);
        if (accountNumber != null) accountNumberField.setText(accountNumber);
    }
    @FXML
    public void topUp() {
        String amountText = amountField.getText();
        String bankName = bankNameField.getText();
        String accountNumber = accountNumberField.getText();
        String method = "card"; // یا "online" بر اساس نیاز
        double amount;
        try {
            amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "هشدار", "مبلغ باید بیشتر از صفر باشد!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "هشدار", "مبلغ نامعتبر است!");
            return;
        }

        CompletableFuture<Map<String, Object>> future = cartService.topUpWallet(token, method, amount, bankName, accountNumber);
        future.thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                double newBalance = data.get("new_balance") != null ? ((Number) data.get("new_balance")).doubleValue() : 0.0;
                showAlert(Alert.AlertType.INFORMATION, "موفقیت", "کیف پول با موفقیت شارژ شد! موجودی جدید: " + newBalance);
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "خطا", response.getOrDefault("message", "خطا در شارژ کیف پول").toString());
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
