package controller;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.OrderResponseDTO;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.*;

public class DeliveryHistoryController {
    @FXML private TableView<OrderResponseDTO> ordersTable;
    @FXML private TableColumn<OrderResponseDTO, String> orderIdColumn;
    @FXML private TableColumn<OrderResponseDTO, String> deliveryAddressColumn;
    @FXML private TableColumn<OrderResponseDTO, String> restaurantColumn;
    @FXML private TableColumn<OrderResponseDTO, Number> payPriceColumn;
    @FXML private TableColumn<OrderResponseDTO, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<OrderResponseDTO, String> statusColumn;
    @FXML private TableColumn<OrderResponseDTO, String> deliveryStatusColumn;
    @FXML private TableColumn<OrderResponseDTO, Void> actionColumn;

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String token;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
        loadDeliveryHistory();
    }

    @FXML
    public void initialize() {
        if (ordersTable == null) {
            System.err.println("Error: ordersTable is null. Check FXML binding.");
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری جدول سفارشات");
            return;
        }

        orderIdColumn.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        deliveryAddressColumn.setCellValueFactory(new PropertyValueFactory<>("deliveryAddress"));
        restaurantColumn.setCellValueFactory(new PropertyValueFactory<>("restaurantName"));
        payPriceColumn.setCellValueFactory(new PropertyValueFactory<>("payPrice"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus() != null ? cellData.getValue().getStatus() : ""));
        deliveryStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDeliveryStatus() != null ? cellData.getValue().getDeliveryStatus() : ""));

        actionColumn.setCellFactory(column -> new TableCell<>() {
            private final Button actionButton = new Button("تغییر وضعیت");

            {
                actionButton.setOnAction(event -> {
                    OrderResponseDTO order = getTableView().getItems().get(getIndex());
                    if (order != null && order.getOrderId() != null) {
                        System.out.println("Changing status for order: " + order.getOrderId());
                        changeDeliveryStatus(order);
                    } else {
                        System.out.println("Order or orderId is null at index: " + getIndex());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(actionButton);
                }
            }
        });
    }

    private void loadDeliveryHistory() {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/deliveries/history"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("History response status: " + response.statusCode());
                    System.out.println("History response body: " + response.body());
                    if (response.statusCode() == 200) {
                        Type listType = new TypeToken<List<OrderResponseDTO>>(){}.getType();
                        List<OrderResponseDTO> orders = gson.fromJson(response.body(), listType);
                        Platform.runLater(() -> {
                            ordersTable.getItems().clear();
                            if (orders == null || orders.isEmpty()) {
                                showAlert(Alert.AlertType.INFORMATION, "اطلاعات", "هیچ سفارشی یافت نشد.");
                            } else {
                                orders.sort((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()));
                                ordersTable.getItems().addAll(orders);
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در دریافت تاریخچه: " + response.body()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + ex.getMessage()));
                    return null;
                });
    }

    private void changeDeliveryStatus(OrderResponseDTO order) {
        System.out.println("Starting status change for order: " + order.getOrderId());
        if (order == null || order.getOrderId() == null) {
            showAlert(Alert.AlertType.ERROR, "خطا", "سفارش یا شناسه آن نامعتبر است!");
            return;
        }

        String currentStatus = order.getDeliveryStatus();
        String newStatus = "received".equalsIgnoreCase(currentStatus) ? "delivered" : "received";

        // استفاده از متد updateOrderStatus
        updateOrderStatus(order.getOrderId(), newStatus);

        // به‌روز کردن وضعیت در شیء order (نیازی به setDeliveryStatus جداگانه نیست، چون refresh انجام می‌شود)
    }

    private void updateOrderStatus(UUID orderId, String newStatus) {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }
        if (orderId == null) {
            showAlert(Alert.AlertType.ERROR, "خطا", "شناسه سفارش نامعتبر است!");
            return;
        }
        if (newStatus == null || !newStatus.matches("accepted|received|delivered")) {
            showAlert(Alert.AlertType.ERROR, "خطا", "وضعیت جدید نامعتبر است: " + newStatus);
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("deliveryStatus", newStatus);
        String requestBody = gson.toJson(body);
        System.out.println("Update deliveryStatus request body: " + requestBody);
        System.out.println("Update deliveryStatus for orderId: " + orderId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/deliveries/" + orderId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Update deliveryStatus response status: " + response.statusCode());
                    System.out.println("Update deliveryStatus response body: " + response.body());
                    if (response.statusCode() == 200) {
                        // به‌روز کردن وضعیت در جدول بدون نیاز به تغییر دستی order
                        Platform.runLater(() -> {
                            ordersTable.refresh(); // جدول را به‌روز می‌کند
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "وضعیت با موفقیت تغییر کرد.");
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در تغییر وضعیت تحویل: " + response.body()));
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Error updating deliveryStatus: " + ex.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + ex.getMessage()));
                    return null;
                });
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }
}