package controller;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import model.TransactionDTO;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class TransactionHistoryController {
    @FXML private ListView<TransactionDTO> transactionListView;
    private String token;
    private Stage stage;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public void setToken(String token) {
        this.token = token;
        loadTransactionHistory();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void loadTransactionHistory() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/transactions/history"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Server responded with status: " + response.statusCode());
                    }
                    Type listType = new TypeToken<List<TransactionDTO>>(){}.getType();
                    List<TransactionDTO> transactions = gson.fromJson(response.body(), listType);
                    return transactions;
                })
                .thenAccept(transactions -> Platform.runLater(() -> {
                    transactionListView.getItems().clear();
                    transactionListView.getItems().addAll(transactions);

                    // تنظیم CellFactory برای نمایش بهتر
                    transactionListView.setCellFactory(lv -> new ListCell<TransactionDTO>() {
                        @Override
                        protected void updateItem(TransactionDTO item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(String.format("مبلغ: %d - نوع: %s - روش: %s - وضعیت: %s - تاریخ: %s",
                                        item.getAmount(),
                                        item.getType(),
                                        item.getMethod(),
                                        item.getStatus(),
                                        item.getCreatedAt()));
                            }
                        }
                    });
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("خطا");
                        alert.setHeaderText(null);
                        alert.setContentText("خطا در دریافت تاریخچه تراکنش‌ها: " + throwable.getMessage());
                        alert.showAndWait();
                    });
                    return null;
                });
    }
}