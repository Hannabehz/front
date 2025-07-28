package controller;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.stage.Modality;
import model.CartItem;
import model.FoodDTO;
import model.MenuResponseDTO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import service.CartService;
import service.RestaurantService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MenuViewController {
    @FXML private Label restaurantNameLabel;
    @FXML private Accordion menuAccordion;
    @FXML private Button backButton;
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private CheckBox friedFilter;
    @FXML private CheckBox appetizerFilter;
    @FXML private CheckBox drinkFilter;
    @FXML private CheckBox fastFoodFilter;
    @FXML private CheckBox spicyFilter;
    @FXML private TextField minRateField;
    @FXML private TextField maxRateField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button applyFiltersButton;
    @FXML private Button clearFiltersButton;

    private Stage stage;
    private String token;
    private UUID restaurantId;
    private String restaurantName;
    private final RestaurantService restaurantService = new RestaurantService();
    private final CartService cartService = new CartService();
    private final Gson gson = new Gson();
    private Map<UUID, Label> quantityLabels = new HashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public void setStage(Stage stage) {
        System.out.println("Setting stage in MenuViewController");
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
        System.out.println("Setting token in MenuViewController: " + token);
        this.token = token;
    }

    public void setRestaurantInfo(UUID restaurantId, String restaurantName) {
        System.out.println("Setting restaurant info: ID=" + restaurantId + ", Name=" + restaurantName);
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        restaurantNameLabel.setText("منوی رستوران: " + restaurantName);
        loadMenus();
    }

    @FXML
    public void initialize() {
        System.out.println("Initializing MenuViewController, restaurantName: " + restaurantName + ", token: " + token);
        sortComboBox.setItems(FXCollections.observableArrayList("امتیاز", "قیمت"));
        // Listener برای اعمال فیلترها هنگام تغییر مرتب‌سازی
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println("Sort changed to: " + newValue);
            if (isTokenValid()) {
                applyFilters();
            }
        });
        // Listener برای اعمال فیلترها هنگام تغییر minRate و maxRate
        minRateField.textProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println("minRate changed to: " + newValue);
            if (isTokenValid()) {
                applyFilters();
            }
        });
        maxRateField.textProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println("maxRate changed to: " + newValue);
            if (isTokenValid()) {
                applyFilters();
            }
        });
        // Listener برای اعمال فیلترها هنگام تغییر CheckBoxها
        for (CheckBox filter : Arrays.asList(friedFilter, appetizerFilter, drinkFilter, fastFoodFilter, spicyFilter)) {
            filter.selectedProperty().addListener((obs, oldValue, newValue) -> {
                System.out.println("Filter changed: " + filter.getText() + " = " + newValue);
                if (isTokenValid()) {
                    applyFilters();
                }
            });
        }
        // Listener برای اعمال فیلترها هنگام تغییر searchField
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            System.out.println("Search text changed to: " + newValue);
            if (isTokenValid()) {
                applyFilters();
            }
        });
    }

    public void loadMenus() {
        if (!isTokenValid()) {
            System.err.println("Token is null or empty in loadMenus");
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست یا هنوز تنظیم نشده است!");
            return;
        }
        if (restaurantId == null) {
            System.err.println("Restaurant ID is null in loadMenus");
            showAlert(Alert.AlertType.ERROR, "خطا", "شناسه رستوران تنظیم نشده است!");
            return;
        }

        System.out.println("Loading menus for restaurant ID: " + restaurantId + " with token: " + token);
        CompletableFuture<MenuResponseDTO> menuFuture = restaurantService.getMenusAndItems(token, restaurantId);

        menuFuture.thenAcceptAsync(menuResponse -> Platform.runLater(() -> {
            menuAccordion.getPanes().clear();
            if (menuResponse == null) {
                System.err.println("Menu response is null");
                showAlert(Alert.AlertType.ERROR, "خطا", "پاسخ سرور برای منوها خالی است!");
                return;
            }

            List<String> menuTitles = menuResponse.getMenu_titles();
            Map<String, List<FoodDTO>> menus = menuResponse.getMenus();

            if (menuTitles == null || menus == null) {
                System.err.println("Menu response is incomplete: titles=" + menuTitles + ", menus=" + menus);
                showAlert(Alert.AlertType.ERROR, "خطا","داده‌های منو ناقص است!");
                return;
            }

            for (String title : menuTitles) {
                VBox content = new VBox(10);
                content.setAlignment(Pos.CENTER_RIGHT);
                List<FoodDTO> items = menus.get(title);
                if (items != null) {
                    for (FoodDTO item : items) {
                        VBox itemBox = new VBox(5);
                        itemBox.setAlignment(Pos.CENTER_RIGHT);
                        itemBox.setPadding(new Insets(10));
                        itemBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1;");

                        ImageView imageView = new ImageView();
                        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
                            try {
                                byte[] imageBytes = Base64.getDecoder().decode(item.getImageBase64());
                                Image image = new Image(new ByteArrayInputStream(imageBytes));
                                imageView.setImage(image);
                                imageView.setFitWidth(100);
                                imageView.setFitHeight(100);
                                imageView.setPreserveRatio(true);
                            } catch (Exception e) {
                                System.err.println("Error loading food image: " + e.getMessage());
                            }
                        }

                        Label nameLabel = new Label("نام: " + item.getName());
                        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                        Label descriptionLabel = new Label("توضیحات: " + (item.getDescription() != null ? item.getDescription() : "بدون توضیحات"));
                        descriptionLabel.setStyle("-fx-font-size: 12px;");
                        Label priceLabel = new Label(String.format("قیمت: %,d تومان", item.getPrice()));
                        priceLabel.setStyle("-fx-font-size: 12px;");
                        Label supplyLabel = new Label("موجودی: " + item.getSupply());
                        supplyLabel.setStyle("-fx-font-size: 12px;");
                        Label categoriesLabel = new Label("دسته‌بندی‌ها: " + String.join(", ", item.getCategories()));
                        categoriesLabel.setStyle("-fx-font-size: 12px;");
                        Label rateLabel = new Label(String.format("امتیاز: %.1f", item.getRate() != null ? item.getRate() : 0.0));
                        rateLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                        Label quantityLabel = new Label("0");
                        quantityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5;");
                        quantityLabels.put(item.getItemId(), quantityLabel);

                        cartService.getCartItemQuantity(token, item.getItemId().toString())
                                .thenAccept(quantity -> Platform.runLater(() -> quantityLabel.setText(String.valueOf(quantity))));

                        Button addButton = new Button("+");
                        addButton.setStyle("-fx-font-size: 12px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
                        addButton.setOnAction(event -> addToCart(item.getItemId(), item.getSupply(), quantityLabel,item.getRestaurantId()));

                        Button removeButton = new Button("-");
                        removeButton.setStyle("-fx-font-size: 12px; -fx-background-color: #F44336; -fx-text-fill: white;");
                        removeButton.setOnAction(event -> removeFromCart(item.getItemId().toString(), quantityLabel,item.getRestaurantId()));
                        Button viewRatingsButton = new Button("مشاهده نظرات");
                        viewRatingsButton.setStyle("-fx-font-size: 12px; -fx-background-color: #2196F3; -fx-text-fill: white;");
                        viewRatingsButton.setOnAction(event -> showRatingsForFood(item.getItemId()));

                        HBox quantityBox = new HBox(5, removeButton, quantityLabel, addButton, viewRatingsButton);
                        quantityBox.setAlignment(Pos.CENTER);

                        itemBox.getChildren().addAll(imageView, nameLabel, descriptionLabel, priceLabel, supplyLabel, categoriesLabel,rateLabel, quantityBox);
                        content.getChildren().add(itemBox);
                    }
                }
                TitledPane pane = new TitledPane(title, content);
                menuAccordion.getPanes().add(pane);
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> {
                String errorMessage = throwable.getMessage();
                System.err.println("Error fetching menus: " + errorMessage);
                showAlert(Alert.AlertType.ERROR, "خطا", "خطا در دریافت منوها: " + errorMessage);
            });
            return null;
        });
    }
    @FXML
    private void searchFoods() {
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        if (!isTokenValid()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }

        Map<String, Object> filters = new HashMap<>();
        String searchText = searchField.getText().trim();
        if (!searchText.isEmpty()) {
            filters.put("search", searchText);
        }

        List<String> categories = new ArrayList<>();
        if (friedFilter.isSelected()) categories.add("سوخاری");
        if (appetizerFilter.isSelected()) categories.add("پیش‌غذا");
        if (drinkFilter.isSelected()) categories.add("نوشیدنی");
        if (fastFoodFilter.isSelected()) categories.add("فست‌فود");
        if (spicyFilter.isSelected()) categories.add("فلفلی");
        if (!categories.isEmpty()) {
            filters.put("categories", categories);
        }

        filters.put("restaurantId", restaurantId.toString());
        String minRateText = minRateField.getText().trim();
        System.out.println("minRate field value: " + minRateText);
        if (!minRateText.isEmpty()) {
            try {
                double minRate = Double.parseDouble(minRateText);
                if (minRate < 0) {
                    showAlert(Alert.AlertType.ERROR, "خطا", "حداقل امتیاز نمی‌تواند منفی باشد!");
                    return;
                }
                filters.put("minRate", minRate);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "خطا", "حداقل امتیاز باید یک عدد معتبر باشد!");
                return;
            }
        }

        // اعتبارسنجی maxRate
        String maxRateText = maxRateField.getText().trim();
        System.out.println("maxRate field value: " + maxRateText);
        if (!maxRateText.isEmpty()) {
            try {
                double maxRate = Double.parseDouble(maxRateText);
                if (maxRate < 0) {
                    showAlert(Alert.AlertType.ERROR, "خطا", "حداکثر امتیاز نمی‌تواند منفی باشد!");
                    return;
                }
                if (!minRateText.isEmpty() && Double.parseDouble(minRateText) > maxRate) {
                    showAlert(Alert.AlertType.ERROR, "خطا", "حداقل امتیاز نمی‌تواند بزرگتر از حداکثر امتیاز باشد!");
                    return;
                }
                filters.put("maxRate", maxRate);
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.ERROR, "خطا", "حداکثر امتیاز باید یک عدد معتبر باشد!");
                return;
            }
        }

        // افزودن sortBy
        String selectedSort = sortComboBox.getSelectionModel().getSelectedItem();
        System.out.println("Selected sort option: " + selectedSort);
        if (selectedSort != null) {
            switch (selectedSort) {
                case "امتیاز":
                    filters.put("sortBy", "rate");
                    break;
                case "قیمت":
                    filters.put("sortBy", "price");
                    break;
                default:
                    System.err.println("Unknown sort option: " + selectedSort);
            }
        }

        System.out.println("Applying filters: " + filters);
        restaurantService.searchItems(token, filters)
                .thenAcceptAsync(foods -> Platform.runLater(() -> {
                    menuAccordion.getPanes().clear();

                    if (foods == null || foods.isEmpty()) {
                        showAlert(Alert.AlertType.INFORMATION, "نتیجه", "هیچ غذایی یافت نشد!");
                        return;
                    }

                    VBox content = new VBox(10);
                    content.setAlignment(Pos.CENTER_RIGHT);

                    for (FoodDTO item : foods) {
                        content.getChildren().add(createFoodItemBox(item));
                    }

                    TitledPane pane = new TitledPane("نتایج جستجو", content);
                    menuAccordion.getPanes().add(pane);
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "خطا", "خطا در جستجو: " + throwable.getMessage()));
                    return null;
                });
    }
    @FXML
    private void clearFilters() {
        searchField.clear();
        friedFilter.setSelected(false);
        appetizerFilter.setSelected(false);
        drinkFilter.setSelected(false);
        fastFoodFilter.setSelected(false);
        spicyFilter.setSelected(false);
        loadMenus();
    }

    private VBox createFoodItemBox(FoodDTO item) {
        VBox itemBox = new VBox(5);
        itemBox.setAlignment(Pos.CENTER_RIGHT);
        itemBox.setPadding(new Insets(10));
        itemBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1;");

        ImageView imageView = new ImageView();
        if (item.getImageBase64() != null && !item.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(item.getImageBase64());
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imageView.setImage(image);
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
            } catch (Exception e) {
                System.err.println("Error loading food image: " + e.getMessage());
            }
        }

        Label nameLabel = new Label("نام: " + item.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        Label descriptionLabel = new Label("توضیحات: " + (item.getDescription() != null ? item.getDescription() : "بدون توضیحات"));
        descriptionLabel.setStyle("-fx-font-size: 12px;");
        Label priceLabel = new Label(String.format("قیمت: %,d تومان", item.getPrice()));
        priceLabel.setStyle("-fx-font-size: 12px;");
        Label supplyLabel = new Label("موجودی: " + item.getSupply());
        supplyLabel.setStyle("-fx-font-size: 12px;");
        Label categoriesLabel = new Label("دسته‌بندی‌ها: " + String.join(", ", item.getCategories()));
        categoriesLabel.setStyle("-fx-font-size: 12px;");
        Label rateLabel = new Label(String.format("امتیاز: %.1f", item.getRate() != null ? item.getRate() : 0.0));
        rateLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        Label quantityLabel = new Label("0");
        quantityLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5;");
        UUID initialCartItemId = item.getItemId();
        quantityLabels.put(initialCartItemId, quantityLabel);

        // به‌روزرسانی مقدار و cartItemId با داده‌های سبد خرید
        cartService.getCartItems(token).thenAccept(items -> Platform.runLater(() -> {
            Optional<CartItem> cartItem = items.stream()
                    .filter(ci -> ci.getItemId().toString().equals(item.getItemId().toString()))
                    .findFirst();
            if (cartItem.isPresent()) {
                quantityLabel.setText(String.valueOf(cartItem.get().getQuantity()));
            }
        }));

        Button addButton = new Button("+");
        addButton.setStyle("-fx-font-size: 12px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        addButton.setOnAction(event -> addToCart(initialCartItemId, item.getSupply(), quantityLabel,item.getRestaurantId()));

        Button removeButton = new Button("-");
        System.out.println("Creating remove button for item: " + initialCartItemId);
        removeButton.setStyle("-fx-font-size: 12px; -fx-background-color: #F44336; -fx-text-fill: white;");
        removeButton.setOnAction(event -> {
            System.out.println("Remove button clicked for item: " + initialCartItemId);
            removeFromCart(initialCartItemId.toString(), quantityLabel,item.getRestaurantId());
        });
        Button viewRatingsButton = new Button("مشاهده نظرات");
        viewRatingsButton.setStyle("-fx-font-size: 12px; -fx-background-color: #2196F3; -fx-text-fill: white;");
        viewRatingsButton.setOnAction(event -> showRatingsForFood(initialCartItemId));

        HBox quantityBox = new HBox(5, removeButton, quantityLabel, addButton, viewRatingsButton);
        quantityBox.setAlignment(Pos.CENTER);

        itemBox.getChildren().addAll(imageView, nameLabel, descriptionLabel, priceLabel, supplyLabel, categoriesLabel,rateLabel, quantityBox);
        return itemBox;
    }
    private void showRatingsForFood(UUID foodId) {
        if (!isTokenValid()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "توکن معتبر نیست!");
            return;
        }

        System.out.println("Fetching ratings for foodId: " + foodId);
        CompletableFuture<List<RatingDTO>> ratingsFuture = fetchRatingsForFood(foodId);

        ratingsFuture.thenAcceptAsync(ratings -> Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("نظرات کاربران");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));
            content.setAlignment(Pos.CENTER_RIGHT);

            if (ratings == null || ratings.isEmpty()) {
                content.getChildren().add(new Label("هیچ نظری برای این غذا ثبت نشده است."));
            } else {
                for (RatingDTO rating : ratings) {
                    VBox ratingBox = new VBox(5);
                    ratingBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10;");

                    Label userLabel = new Label("کاربر: " + rating.getUserName());
                    userLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                    Label ratingLabel = new Label("امتیاز: " + rating.getRating());
                    ratingLabel.setStyle("-fx-font-size: 12px;");
                    Label commentLabel = new Label("نظر: " + rating.getComment());
                    commentLabel.setStyle("-fx-font-size: 12px;");
                    commentLabel.setWrapText(true);

                    ImageView imageView = new ImageView();
                    if (rating.getImageBase64() != null && !rating.getImageBase64().isEmpty()) {
                        try {
                            byte[] imageBytes = Base64.getDecoder().decode(rating.getImageBase64());
                            Image image = new Image(new ByteArrayInputStream(imageBytes));
                            imageView.setImage(image);
                            imageView.setFitWidth(100);
                            imageView.setFitHeight(100);
                            imageView.setPreserveRatio(true);
                        } catch (Exception e) {
                            System.err.println("Error loading rating image: " + e.getMessage());
                        }
                    }

                    ratingBox.getChildren().addAll(userLabel, ratingLabel, commentLabel, imageView);
                    content.getChildren().add(ratingBox);
                }
            }

            ButtonType closeButton = new ButtonType("بستن", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().add(closeButton);
            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در دریافت نظرات: " + throwable.getMessage()));
            throwable.printStackTrace();
            return null;
        });
    }

    private CompletableFuture<List<RatingDTO>> fetchRatingsForFood(UUID foodId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/ratings/food/" + foodId))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Server responded with status: " + response.statusCode());
                    }
                    Type listType = new TypeToken<List<RatingDTO>>(){}.getType();
                    return gson.fromJson(response.body(), listType);
                });
    }
    public static class RatingDTO {
        private UUID id;
        private Integer rating;
        private String comment;
        private String imageBase64;
        private String userName;
        private UUID orderId;
        public RatingDTO() {
        }

        public RatingDTO(UUID id, Integer rating, String comment, String imageBase64, String userName, UUID orderId) {
            this.id = id;
            this.rating = rating;
            this.comment = comment;
            this.imageBase64 = imageBase64;
            this.userName = userName;
            this.orderId = orderId;
        }
        public UUID getId() { return id; }
        public Integer getRating() { return rating; }
        public String getComment() { return comment; }
        public String getImageBase64() { return imageBase64; }
        public String getUserName() { return userName; }
        public UUID getOrderId() { return orderId; }
        public void setId(UUID id) { this.id = id; }
        public void setRating(Integer rating) { this.rating = rating; }
        public void setComment(String comment) { this.comment = comment; }
        public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }
        public void setUserName(String userName) { this.userName = userName; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }
    }
    private void addToCart(UUID itemUuid, int supply, Label quantityLabel, UUID restaurantId) { // اضافه کردن restaurantId
        int currentQuantity = Integer.parseInt(quantityLabel.getText());
        if (currentQuantity >= supply) {
            showAlert(Alert.AlertType.WARNING, "هشدار", "نمی‌توانید بیشتر از موجودی (" + supply + ") اضافه کنید!");
            return;
        }
        System.out.println("Adding item to cart with itemUuid: " + itemUuid + ", restaurantId: " + restaurantId);
        cartService.addToCart(token, itemUuid, 1, restaurantId).thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response == null || !response.containsKey("status")) {
                showAlert(Alert.AlertType.ERROR, "خطا", "پاسخ سرور نامعتبر است");
                return;
            }
            int status = ((Number) response.get("status")).intValue();
            if (status == 200) {
                quantityLabel.setText(String.valueOf(currentQuantity + 1));
                System.out.println("Item added to cart successfully: " + itemUuid);
            } else {
                String message = response.getOrDefault("message", response.getOrDefault("error", "خطا در افزودن به سبد خرید")).toString();
                showAlert(Alert.AlertType.ERROR, "خطا", message);
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }

    private void removeFromCart(String itemId, Label quantityLabel, UUID restaurantId) { // اضافه کردن restaurantId
        int currentQuantity = Integer.parseInt(quantityLabel.getText());
        if (currentQuantity <= 0) {
            return;
        }
        UUID itemUuid = UUID.fromString(itemId);
        System.out.println("Removing item: " + itemId + ", token: " + (token != null ? token.substring(0, 10) + "..." : "null") + ", restaurantId: " + restaurantId);
        cartService.removeFromCart(token, itemUuid, restaurantId).thenAcceptAsync(response -> Platform.runLater(() -> {
            if (response == null || !response.containsKey("status")) {
                showAlert(Alert.AlertType.ERROR, "خطا", "پاسخ سرور نامعتبر است");
                return;
            }
            int status = ((Number) response.get("status")).intValue();
            if (status == 200) {
                quantityLabel.setText(String.valueOf(currentQuantity - 1));
                System.out.println("Item removed from cart: " + itemId);
            } else {
                String message = response.getOrDefault("message", response.getOrDefault("error", "خطا در حذف از سبد خرید")).toString();
                showAlert(Alert.AlertType.ERROR, "خطا", message);
            }
        })).exceptionally(throwable -> {
            System.err.println("Remove from cart exception: " + throwable.getMessage());
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
            return null;
        });
    }
    @FXML
    public void goBack() {
        try {
            String fxmlPath = "/org/example/demo1/mainView.fxml";
            java.net.URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                throw new IOException("Cannot find FXML file at path: " + fxmlPath);
            }
            System.out.println("Loading FXML from: " + fxmlUrl);
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            MainViewController controller = new MainViewController();
            loader.setController(controller);
            Parent root = loader.load();
            controller.setStage(stage);
            controller.setToken(token);
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setX(0);
            stage.setY(0);
            stage.setWidth(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth());
            stage.setHeight(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight());
            stage.show();
            System.out.println("Navigated back to MainView"); }
        catch (IOException e) {
            System.err.println("Error loading main view: "
                    + e.getMessage()); showAlert(Alert.AlertType.ERROR, "خطا",
                    "خطا در بازگشت به صفحه اصلی: " + e.getMessage()); }
    }


    private boolean isTokenValid() {
        return token != null && !token.trim().isEmpty();
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