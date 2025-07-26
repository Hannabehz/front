package controller;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import service.CartService;
import service.UserService;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PaymentGatewayController {
    @FXML private Label amountLabel;
    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;
    @FXML private Button payButton;

    private final CartService cartService = new CartService();
    private final Gson gson = new Gson();
    private String token;
    private String orderId;
    private double amount;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setAmount(double amount) {
        this.amount = amount;
        amountLabel.setText("مبلغ: " + String.format("%.2f تومان", amount));
    }

    public void setBankDetails(String bankName, String accountNumber) {
        if (bankName != null) bankNameField.setText(bankName);
        if (accountNumber != null) accountNumberField.setText(accountNumber);
    }

    @FXML
    private void pay() {
        String bankName = bankNameField.getText();
        String accountNumber = accountNumberField.getText();
        String method = "paywall";

        CompletableFuture<Map<String, Object>> future = cartService.makeOnlinePayment(token, orderId, method, bankName, accountNumber);
        future.thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "موفقیت", "پرداخت با موفقیت انجام شد!");
                stage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "خطا", response.getOrDefault("message", "خطا در پرداخت").toString());
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }
    private void loadUserProfile() {
        UserService userService = new UserService();
        CompletableFuture<Map<String, Object>> future = userService.getProfile(token);; // فرضی
        future.thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                Map<String, Object> user = (Map<String, Object>) response.get("user");
                setBankDetails(
                        user.containsKey("bankName") ? (String) user.get("bankName") : null,
                        user.containsKey("accountNumber") ? (String) user.get("accountNumber") : null
                );
            }
        }));
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}