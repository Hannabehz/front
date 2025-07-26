package controller;

import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Order;
import service.UserService;
import view.LoginView;

import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CourierMainViewController {
    @FXML private MenuItem showProfileItem;
    @FXML private MenuItem logOutButton;
    @FXML private MenuItem viewDeliveryHistoryItem;
    @FXML private MenuButton menuButton;
    @FXML private TableView<Order> ordersTable;
    @FXML private TableColumn<Order, String> orderIdColumn;
    @FXML private TableColumn<Order, String> deliveryAddressColumn;
    @FXML private TableColumn<Order, String> restaurantColumn;
    @FXML private TableColumn<Order, Number> payPriceColumn;
    @FXML private TableColumn<Order, String> createdAtColumn;
    @FXML private TableColumn<Order, String> statusColumn;

    private String token;
    private Stage stage;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson;

    public CourierMainViewController(Stage stage, String token) {
        this.stage = stage;
        this.token = token;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        gson = gsonBuilder.create();
    }

    @FXML
    public void initialize() {
        // بررسی متغیرهای @FXML برای دیباگ
        if (menuButton == null) System.err.println("menuButton is null");
        if (showProfileItem == null) System.err.println("showProfileItem is null");
        if (logOutButton == null) System.err.println("logOutButton is null");
        if (viewDeliveryHistoryItem == null) System.err.println("viewDeliveryHistoryItem is null");
        if (ordersTable == null) System.err.println("ordersTable is null");
        if (orderIdColumn == null) System.err.println("orderIdColumn is null");
        if (deliveryAddressColumn == null) System.err.println("deliveryAddressColumn is null");
        if (restaurantColumn == null) System.err.println("restaurantColumn is null");
        if (payPriceColumn == null) System.err.println("payPriceColumn is null");
        if (createdAtColumn == null) System.err.println("createdAtColumn is null");
        if (statusColumn == null) System.err.println("statusColumn is null");

        // اگر اجزای اصلی null باشند، از ادامه اجرا جلوگیری می‌کنیم
        if (menuButton == null || ordersTable == null || statusColumn == null) {
            System.err.println("One or more critical FXML components are null. Check FXML file.");
            return;
        }

        // تنظیم cellValueFactory برای statusColumn با استفاده از deliveryStatus
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDeliveryStatus() != null ? cellData.getValue().getDeliveryStatus() : ""
        ));

        // تنظیم cellFactory برای statusColumn (برای ComboBox)
        statusColumn.setCellFactory(column -> new TableCell<Order, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            @Override
            protected void updateItem(String deliveryStatus, boolean empty) {
                super.updateItem(deliveryStatus, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                comboBox.getItems().clear();
                String currentStatus = deliveryStatus != null ? deliveryStatus : "";
                System.out.println("Current deliveryStatus for order: " + getTableRow().getItem().getId() + " = " + currentStatus);

                // مدیریت مقادیر deliveryStatus
                if (currentStatus.isEmpty()) {
                    comboBox.getItems().add("accepted");
                } else if (currentStatus.equals("accepted")) {
                    comboBox.getItems().addAll("received", "delivered");
                } else if (currentStatus.equals("received")) {
                    comboBox.getItems().add("delivered");
                }

                comboBox.setValue(currentStatus.isEmpty() ? "waiting for courier" : currentStatus);
                comboBox.setOnAction(event -> {
                    String newStatus = comboBox.getValue();
                    Order order = getTableView().getItems().get(getIndex());
                    System.out.println("Updating deliveryStatus for order " + order.getId() + " to: " + newStatus);
                    updateOrderStatus(order.getId().toString(), newStatus);
                });

                setGraphic(comboBox);
            }
        });

        // تنظیم cellValueFactory برای سایر ستون‌ها
        orderIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getId().toString()));
        deliveryAddressColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDeliveryAddress()));
        restaurantColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRestaurantName()));
        payPriceColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getPayPrice()));
        createdAtColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getCreatedAt() != null
                        ? cellData.getValue().getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : ""
        ));

        ordersTable.setPlaceholder(new Label("هیچ سفارشی در دسترس نیست"));
        showAvailableDeliveries();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    private void showProfile() {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست یا وجود ندارد!");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/auth/profile"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Map<String, Object> userData = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>() {}.getType());
                        Platform.runLater(() -> {
                            try {
                                FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/displayProfile.fxml"));
                                Parent root = loader.load();
                                DisplayProfileController controller = loader.getController();
                                controller.setToken(token);
                                controller.setProfileData(userData);
                                Stage profileStage = new Stage();
                                profileStage.setScene(new Scene(root, 600, 400));
                                profileStage.setTitle("نمایش پروفایل");
                                profileStage.show();
                            } catch (IOException e) {
                                showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری صفحه پروفایل: " + e.getMessage());
                            }
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در دریافت پروفایل: " + response.body()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void logOut() {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست یا وجود ندارد!");
            return;
        }
        UserService userService=new UserService();
        userService.logOut(token).thenAccept(response -> Platform.runLater(() -> {
            System.out.println("Logout response: " + response);
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                token = null; // پاک کردن توکن// حذف صحنه فعلی
                if (stage != null) {
                    stage.close();
                } else {
                    System.err.println("Stage is null in MainViewController.logOut!");
                }

                // باز کردن LoginView در Stage جدید
                try {
                    Stage newStage = new Stage();
                    LoginView loginView = new LoginView(newStage);
                } catch (Exception e) {
                    System.err.println("Error creating LoginView: " + e.getMessage());
                    e.printStackTrace();
                    showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری صفحه ورود: " + e.getMessage());
                }

            } else {
                String message = response.getOrDefault("message", "خطا در خروج از حساب").toString();
                showAlert(Alert.AlertType.ERROR, "خطا", message);
            }
        }));
    }

    @FXML
    private void showAvailableDeliveries() {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن کاربر یافت نشد");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/delivery/available"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    System.out.println("Available deliveries response status: " + response.statusCode());
                    System.out.println("Available deliveries response body: " + response.body());
                    if (response.statusCode() == 200) {
                        List<Order> orders = gson.fromJson(response.body(), new TypeToken<List<Order>>() {}.getType());
                        Platform.runLater(() -> {
                            ordersTable.getItems().clear();
                            ordersTable.getItems().addAll(orders);
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در دریافت سفارش‌ها: " + response.body()));
                    }
                })
                .exceptionally(ex -> {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + ex.getMessage()));
                    return null;
                });
    }

    @FXML
    private void showDeliveryHistory() {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/deliveryHistoryView.fxml"));
            Stage deliveryHistoryStage = new Stage();
            deliveryHistoryStage.initModality(Modality.APPLICATION_MODAL);
            deliveryHistoryStage.initOwner(stage);
            deliveryHistoryStage.setTitle("تاریخچه تحویل‌ها");
            deliveryHistoryStage.setScene(new Scene(loader.load()));
            DeliveryHistoryController controller = loader.getController();
            controller.setStage(deliveryHistoryStage);
            controller.setToken(token);
            deliveryHistoryStage.showAndWait();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری تاریخچه: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        if (token == null || token.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "شناسه سفارش نامعتبر است!");
            return;
        }
        if (newStatus == null || !newStatus.matches("accepted|received|delivered")) {
            showAlert(Alert.AlertType.ERROR, "خطا", "وضعیت جدید نامعتبر است: " + newStatus);
            return;
        }

        Map<String, String> body = new HashMap<>();
        body.put("deliveryStatus", newStatus); // تغییر به deliveryStatus
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
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "وضعیت تحویل سفارش با موفقیت تغییر کرد.");
                            showAvailableDeliveries();
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}