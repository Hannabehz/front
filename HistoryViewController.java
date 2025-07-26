package controller;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class HistoryViewController {
    @FXML private TableView<OrderResponseDTO> ordersTable;
    @FXML private TableColumn<OrderResponseDTO, String> orderIdColumn;
    @FXML private TableColumn<OrderResponseDTO, String> deliveryAddressColumn;
    @FXML private TableColumn<OrderResponseDTO, String> restaurantColumn;
    @FXML private TableColumn<OrderResponseDTO, Number> payPriceColumn;
    @FXML private TableColumn<OrderResponseDTO, LocalDateTime> createdAtColumn;
    @FXML private TableColumn<OrderResponseDTO, String> statusColumn; // ستون جدید برای status
    @FXML private TableColumn<OrderResponseDTO, String> deliveryStatusColumn; // ستون جدید برای deliveryStatus
    @FXML private TableColumn<OrderResponseDTO, Void> ratingColumn;
    private final Gson gson;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String token;
    private Stage stage;

    public HistoryViewController() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        gson = gsonBuilder.create();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
        System.out.println("Token set in HistoryViewController: " + token);
        loadOrderHistory();
    }

    @FXML
    public void initialize() {
        if (ordersTable == null) {
            System.err.println("Error: ordersTable is null. Check FXML binding.");
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری جدول سفارشات");
            return;
        }

        orderIdColumn.setCellValueFactory(cellData -> {
            UUID orderId = cellData.getValue().getOrderId();
            return new SimpleStringProperty(orderId != null ? orderId.toString() : "");
        });

        deliveryAddressColumn.setCellValueFactory(cellData -> {
            String address = cellData.getValue().getDeliveryAddress();
            return new SimpleStringProperty(address != null ? address : "");
        });

        restaurantColumn.setCellValueFactory(cellData -> {
            String name = cellData.getValue().getRestaurantName();
            return new SimpleStringProperty(name != null ? name : "");
        });

        payPriceColumn.setCellValueFactory(cellData -> {
            Double price = cellData.getValue().getPayPrice();
            return new SimpleDoubleProperty(price != null ? price : 0.0);
        });

        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreatedAt();
            return new SimpleObjectProperty<>(date != null ? date : LocalDateTime.now());
        });

        createdAtColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(formatter.format(item));
                }
            }
        });

        // تنظیم ستون جدید برای status
        statusColumn.setCellValueFactory(cellData -> {
            String status = cellData.getValue().getStatus();
            return new SimpleStringProperty(status != null ? status : "");
        });

        // تنظیم ستون جدید برای deliveryStatus
        deliveryStatusColumn.setCellValueFactory(cellData -> {
            String deliveryStatus = cellData.getValue().getDeliveryStatus();
            return new SimpleStringProperty(deliveryStatus != null ? deliveryStatus : "");
        });

        ratingColumn.setCellFactory(column -> new TableCell<>() {
            private final Button rateButton = new Button("امتیاز دهید");

            {
                rateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                rateButton.setOnAction(event -> {
                    OrderResponseDTO order = getTableView().getItems().get(getIndex());
                    if (order != null && order.getOrderId() != null) {
                        openRatingDialog(order.getOrderId().toString());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    OrderResponseDTO order = getTableView().getItems().get(getIndex());
                    if (order != null && "delivered".equalsIgnoreCase(order.getDeliveryStatus())) {
                        setGraphic(rateButton);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadOrderHistory() {
        if (token == null || token.trim().isEmpty()) {
            System.err.println("Error: Token is null or empty in loadOrderHistory");
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن احراز هویت موجود نیست");
            return;
        }

        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/orders/history"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                String body = response.body();
                System.out.println("Raw response from /orders/history: " + body);

                if (response.statusCode() == 200) {
                    Type listType = new TypeToken<List<OrderResponseDTO>>(){}.getType();
                    List<OrderResponseDTO> orders = gson.fromJson(body, listType);

                    System.out.println("Parsed orders: " + orders);
                    if (orders != null) {
                        orders.forEach(order -> System.out.println(
                                "Order ID: " + order.getOrderId() +
                                        ", Restaurant: " + order.getRestaurantName() +
                                        ", Price: " + order.getPayPrice() +
                                        ", Status: " + order.getStatus() + // لاگ برای دیباگ
                                        ", Delivery Status: " + order.getDeliveryStatus() // لاگ برای دیباگ
                        ));
                    }

                    return orders != null ? orders : new ArrayList<OrderResponseDTO>();
                } else {
                    Platform.runLater(() -> showAlert(
                            Alert.AlertType.ERROR,
                            "خطا",
                            "خطا در دریافت تاریخچه سفارشات: کد " + response.statusCode() +
                                    "\nپیام: " + body
                    ));
                    return new ArrayList<OrderResponseDTO>();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert(
                        Alert.AlertType.ERROR,
                        "خطا",
                        "خطا در ارتباط با سرور: " + e.getMessage()
                ));
                return new ArrayList<OrderResponseDTO>();
            }
        }).thenAcceptAsync(receivedOrders -> Platform.runLater(() -> {
            List<OrderResponseDTO> ordersToShow = receivedOrders != null ? receivedOrders : new ArrayList<>();

            ordersTable.getItems().clear();
            ordersTable.getItems().addAll(ordersToShow);

            System.out.println("Loaded " + ordersToShow.size() + " orders into the table");
        }));
    }

    private void openRatingDialog(String orderId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/ratingDialog.fxml"));
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(stage);
            dialogStage.setTitle("ثبت نظر");
            dialogStage.setScene(new Scene(loader.load()));
            RatingDialogController controller = loader.getController();
            controller.setStage(dialogStage);
            controller.setToken(token);
            controller.setOrderId(orderId);
            dialogStage.showAndWait();
            loadOrderHistory();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در باز کردن پنجره ثبت نظر: " + e.getMessage());
        }
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