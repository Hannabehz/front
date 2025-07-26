package controller;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import model.CartItem;
import service.CartService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CartViewController {
    @FXML private TableView<CartItem> cartTable;
    @FXML private TableColumn<CartItem, String> nameColumn;
    @FXML private TableColumn<CartItem, ImageView> imageColumn;
    @FXML private TableColumn<CartItem, String> descriptionColumn;
    @FXML private TableColumn<CartItem, Number> priceColumn;
    @FXML private TableColumn<CartItem, Number> supplyColumn;
    @FXML private TableColumn<CartItem, String> categoriesColumn;
    @FXML private TableColumn<CartItem, Number> quantityColumn;
    @FXML private TableColumn<CartItem, Void> actionColumn;
    @FXML private Label totalPriceLabel;
    @FXML private Button submitOrder;
    @FXML private Button topUpWalletButton;
    @FXML private TextField deliveryAddressField;

    private final CartService cartService = new CartService();
    private final Gson gson = new Gson();
    private String token;
    private String bankName;
    private String accountNumber;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        if (stage != null) {
            stage.setMaximized(true);
            stage.setX(0);
            stage.setY(0);
            stage.setWidth(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth());
            stage.setHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight());
        }
    }

    public void setToken(String token) {
        this.token = token;
        System.out.println("Token set in CartViewController: " + (token != null ? token.substring(0, 10) + "..." : "null") + " at " + new java.util.Date());
        if (token != null && !token.trim().isEmpty()) {
            loadCart();
        } else {
            System.out.println("Token is null or empty, cart load skipped at " + new java.util.Date());
        }
    }

    public void setBankDetails(String bankName, String accountNumber) {
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }

    @FXML
    public void initialize() {
        if (cartTable == null) {
            System.err.println("Error: cartTable is null. Check FXML binding.");
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری جدول سبد خرید");
            return;
        }
        if (deliveryAddressField == null) {
            System.err.println("Error: deliveryAddressField is null. Check FXML binding.");
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری فیلد آدرس تحویل");
            return;
        }

        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        descriptionColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));
        priceColumn.setCellValueFactory(cellData -> new SimpleDoubleProperty(cellData.getValue().getPrice()));
        supplyColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getSupply()));
        categoriesColumn.setCellValueFactory(cellData -> {
            List<String> categories = cellData.getValue().getCategories();
            return new SimpleStringProperty(categories != null ? String.join(", ", categories) : "");
        });
        quantityColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getQuantity()));
        imageColumn.setCellValueFactory(cellData -> {
            String base64 = cellData.getValue().getImageBase64();
            ImageView imageView = new ImageView();
            imageView.setFitWidth(100);
            imageView.setFitHeight(100);
            if (base64 != null && !base64.isEmpty()) {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    imageView.setImage(image);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid Base64 string for item: " + cellData.getValue().getName());
                }
            }
            return new SimpleObjectProperty<>(imageView);
        });
        actionColumn.setCellFactory(param -> new TableCell<>() {
            private final Button removeButton = new Button("حذف");

            {
                removeButton.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    cartService.removeFromCart(token, item.getItemId(),item.getRestaurantId())
                            .thenAccept(response -> Platform.runLater(() -> {
                                if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                                    // به جای حذف مستقیم، سبد خرید را دوباره لود کن
                                    loadCart();
                                } else {
                                    showAlert(Alert.AlertType.ERROR, "خطا", response.getOrDefault("message", "خطا در حذف از سبد خرید").toString());
                                }
                            }));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(removeButton);
                }
            }
        });

        updateTotalPrice();
    }

    @FXML
    private void submitOrder() {
        if (cartTable == null || cartTable.getItems().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "هشدار", "سبد خرید خالی است!");
            return;
        }

        String deliveryAddress = deliveryAddressField.getText();
        if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "هشدار", "لطفاً آدرس تحویل را وارد کنید!");
            return;
        }

        UUID restaurantId = cartTable.getItems().get(0).getRestaurantId(); // تغییر از vendorId به restaurantId
        if (restaurantId == null) {
            showAlert(Alert.AlertType.ERROR, "خطا", "شناسه رستوران معتبر نیست!");
            return;
        }

        boolean allSameRestaurant = cartTable.getItems().stream()
                .allMatch(item -> item.getRestaurantId().equals(restaurantId)); // تغییر از vendorId به restaurantId
        if (!allSameRestaurant) {
            showAlert(Alert.AlertType.ERROR, "خطا", "همه آیتم‌ها باید از یک رستوران باشند!");
            return;
        }

        if (cartTable.getItems().stream().anyMatch(item -> item.getItemId() == null)) {
            showAlert(Alert.AlertType.ERROR, "خطا", "شناسه برخی آیتم‌ها معتبر نیست!");
            return;
        }

        List<Map<String, Object>> items = cartTable.getItems().stream()
                .map(item -> {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("item_id", item.getItemId());
                    itemMap.put("quantity", item.getQuantity());
                    return itemMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("delivery_address", deliveryAddress);
        requestBody.put("restaurant_id", restaurantId.toString()); // تغییر از vendor_id به restaurant_id
        requestBody.put("items", items);

        CompletableFuture<Map<String, Object>> future = cartService.submitOrder(token, requestBody);
        future.thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response == null) {
                showAlert(Alert.AlertType.ERROR, "خطا", "پاسخ سرور نامعتبر است");
                return;
            }
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data == null) {
                    showAlert(Alert.AlertType.ERROR, "خطا", "داده‌های سفارش نامعتبر است");
                    return;
                }
                String orderId = (String) data.get("id");
                double payPrice = data.get("pay_price") != null ? ((Number) data.get("pay_price")).doubleValue() : 0.0;

                showPaymentOptions(orderId, payPrice);
            } else {
                showAlert(Alert.AlertType.ERROR, "خطا", response.getOrDefault("error", response.getOrDefault("message", "خطا در ثبت سفارش")).toString());
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }

    private void showPaymentOptions(String orderId, double payPrice) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("انتخاب روش پرداخت");
        alert.setHeaderText("لطفاً روش پرداخت را انتخاب کنید");
        alert.setContentText("مبلغ قابل پرداخت: " + String.format("%.2f", payPrice));

        ButtonType walletButton = new ButtonType("پرداخت از کیف پول");
        ButtonType paywallButton = new ButtonType("پرداخت مستقیم");
        ButtonType cancelButton = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(walletButton, paywallButton, cancelButton);

        alert.showAndWait().ifPresent(response -> {
            if (response == walletButton) {
                makePayment(orderId, "wallet", payPrice);
            } else if (response == paywallButton) {
                showPaymentGatewayForm(orderId, payPrice);
            }
        });
    }

    private void makePayment(String orderId, String method, double payPrice) {
        CompletableFuture<Map<String, Object>> future = cartService.makeOnlinePayment(token, orderId, method, bankName, accountNumber);
        future.thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response == null) {
                showAlert(Alert.AlertType.ERROR, "خطا", "پاسخ سرور نامعتبر است");
                return;
            }
            if (response.containsKey("status") && ((Number) response.get("status")).intValue() == 200) {
                showAlert(Alert.AlertType.INFORMATION, "موفقیت", "پرداخت با موفقیت انجام شد!");
                cartTable.getItems().clear();
                updateTotalPrice();
                deliveryAddressField.clear();
            } else if (response.containsKey("error") && response.get("error").equals("Insufficient balance")) {
                Alert retryAlert = new Alert(Alert.AlertType.CONFIRMATION);
                retryAlert.setTitle("موجودی ناکافی");
                retryAlert.setHeaderText("موجودی کیف پول کافی نیست!");
                retryAlert.setContentText("آیا می‌خواهید کیف پول خود را شارژ کنید یا از درگاه پرداخت استفاده کنید؟");

                ButtonType topUpButton = new ButtonType("شارژ کیف پول");
                ButtonType paywallButton = new ButtonType("پرداخت مستقیم");
                ButtonType cancelButton = new ButtonType("لغو", ButtonBar.ButtonData.CANCEL_CLOSE);

                retryAlert.getButtonTypes().setAll(topUpButton, paywallButton, cancelButton);

                retryAlert.showAndWait().ifPresent(retryResponse -> {
                    if (retryResponse == topUpButton) {
                        topUpWallet();
                    } else if (retryResponse == paywallButton) {
                        showPaymentGatewayForm(orderId, payPrice);
                    }
                });
            } else {
                showAlert(Alert.AlertType.ERROR, "خطا", response.getOrDefault("message", "خطا در پرداخت").toString());
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }

    private void showPaymentGatewayForm(String orderId, double amount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/paymentGateway.fxml"));
            Parent root = loader.load();
            PaymentGatewayController controller = loader.getController();
            controller.setToken(token);
            controller.setAmount(amount);
            controller.setOrderId(orderId);
            controller.setBankDetails(bankName, accountNumber);
            Stage paymentStage = new Stage();
            controller.setStage(paymentStage);
            paymentStage.setScene(new Scene(root, 400, 300));
            paymentStage.setTitle("پرداخت از درگاه");
            paymentStage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری صفحه پرداخت: " + e.getMessage());
        }
    }

    @FXML
    private void topUpWallet() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/walletTopUp.fxml"));
            Parent root = loader.load();
            WalletTopUpController controller = loader.getController();
            controller.setToken(token);
            controller.setBankDetails(bankName, accountNumber);
            Stage walletStage = new Stage();
            controller.setStage(walletStage);
            walletStage.setScene(new Scene(root, 400, 300));
            walletStage.setTitle("شارژ کیف پول");
            walletStage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری صفحه شارژ کیف پول: " + e.getMessage());
        }
    }

    private void loadCart() {
        CompletableFuture<List<CartItem>> future = cartService.getCartItems(token);
        future.thenAcceptAsync(items -> Platform.runLater(() -> {
            cartTable.getItems().clear();
            if (items == null || items.isEmpty()) {
                System.out.println("No items found in cart or response is null");
            } else {
                cartTable.getItems().addAll(items);
            }
            updateTotalPrice();
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در لود سبد خرید: " + throwable.getMessage()));
            return null;
        });
    }

    private void updateTotalPrice() {
        double total = cartTable.getItems().stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
        totalPriceLabel.setText(String.format("%.2f", total));
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