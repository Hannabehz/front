package service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import model.FoodDTO;
import model.MenuResponseDTO;
import model.RestaurantDTO;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RestaurantService {
    private static final String BASE_URL = "http://localhost:8080";
    private final Gson gson = new Gson();
    private final HttpClient client = HttpClient.newHttpClient();

    //  ساخت رستورا
    public CompletableFuture<Map<String, Object>> createRestaurant(RestaurantDTO restaurant, String token) {
        String json = gson.toJson(restaurant);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseJsonResponse(response));
    }
    public CompletableFuture<Map<String, Object>> getRestaurantById(int id, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + id))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
                    result.put("status", response.statusCode());
                    return result;
                });
    }
    //  دریافت لیست رستوران‌های فروشنده


        public CompletableFuture<List<Map<String, Object>>> getSellerRestaurants(String token) {
        /*  کد واقعی برای اتصال به سرور
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/restaurants/mine"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "application/json")
            .GET()
            .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>() {}.getType()));
        */

            //  داده تستی برای اجرا در حالت آفلاین
            List<Map<String, Object>> sampleData = List.of(
                    Map.of("id", "1", "name", "رستوران تستی", "address", "تهران", "phone", "09121234567", "tax_fee", "5", "additional_fee", "3000"),
                    Map.of("id", "2", "name", "فست‌فود شبانه", "address", "اصفهان", "phone", "09361234567", "tax_fee", "4", "additional_fee", "2500")
            );

            return CompletableFuture.completedFuture(sampleData);
        }
    //  بروزرسانی اطلاعات رستوران
    public CompletableFuture<Map<String, Object>> updateRestaurant(int restaurantId, RestaurantDTO updatedData, String token) {
        String json = gson.toJson(updatedData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + restaurantId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseJsonResponse(response));
    }

    //  متد کمکی برای تبدیل پاسخ به Map و افزودن statusCode
    private Map<String, Object> parseJsonResponse(HttpResponse<String> response) {
        Map<String, Object> result = gson.fromJson(response.body(), new TypeToken<Map<String, Object>>(){}.getType());
        result.put("status", response.statusCode());
        return result;
    }

    // 🥘 افزودن آیتم غذا به رستوران
    public CompletableFuture<Map<String, Object>> addFoodItem(int restaurantId, Map<String, Object> itemData, String token) {
        String json = gson.toJson(itemData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + restaurantId + "/item"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }
    public CompletableFuture<List<Map<String, Object>>> getRestaurantMenu(String token, int restaurantId) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + restaurantId + "/menu"))
                .header("Authorization", token)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>() {}.getType()));
    }
    // ️ ویرایش آیتم غذا
    public CompletableFuture<Map<String, Object>> editFoodItem(int itemId, Map<String, Object> newItemData, String token) {
        String json = gson.toJson(newItemData);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + itemId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }

    // ️ حذف آیتم غذا
    public CompletableFuture<Map<String, Object>> deleteFoodItem(int itemId, String token) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + itemId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }

    // ️ افزودن منو برای رستوران
    public CompletableFuture<Map<String, Object>> addMenu(int restaurantId, String title, String token) {
        Map<String, Object> body = Map.of("title", title);
        String json = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/" + restaurantId + "/menu"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }

    // ️ حذف منو از رستوران
    public CompletableFuture<Map<String, Object>> deleteMenu(int restaurantId, String title, String token) {
        String uri = BASE_URL + "/restaurants/" + restaurantId + "/menu/" + title;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }

    // افزودن آیتم به منوی خاص
    public CompletableFuture<Map<String, Object>> addItemToMenu(int restaurantId, String title, int itemId, String token) {
        Map<String, Object> body = Map.of("item_id", itemId);
        String json = gson.toJson(body);
        String uri = BASE_URL + "/restaurants/" + restaurantId + "/menu/" + title;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }

    // ️ حذف آیتم از منوی خاص
    public CompletableFuture<Map<String, Object>> removeItemFromMenu(int restaurantId, String title, int itemId, String token) {
        String uri = BASE_URL + "/restaurants/" + restaurantId + "/menu/" + title + "/" + itemId;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }
    //  دریافت لیست سفارش‌های یک رستوران
    public CompletableFuture<List<Map<String, Object>>> getRestaurantOrders(int restaurantId, Map<String, String> filters, String token) {
        StringBuilder query = new StringBuilder("?");
        if (filters != null && !filters.isEmpty()) {
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                query.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }

        String uri = BASE_URL + "/restaurants/" + restaurantId + "/orders" + (query.length() > 1 ? query.toString() : "");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>(){}.getType()));
    }

    //  تغییر وضعیت سفارش خاص
    public CompletableFuture<Map<String, Object>> updateOrderStatus(int orderId, String status, String token) {
        Map<String, String> body = Map.of("status", status);
        String json = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/restaurants/orders/" + orderId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::parseJsonResponse);
    }
    public CompletableFuture<List<RestaurantDTO>> getRestaurants(String token, String search, List<String> categories, String sortBy) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL("http://localhost:8080/vendors");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setDoOutput(true);

                Map<String, Object> requestBody = new HashMap<>();
                if (search != null && !search.trim().isEmpty()) {
                    requestBody.put("search", search);
                }
                if (categories != null && !categories.isEmpty()) {
                    requestBody.put("categories", categories);
                }
                if (sortBy != null) {
                    requestBody.put("sortBy", sortBy);
                }
                String jsonInputString = gson.toJson(requestBody);
                System.out.println("Sending request to /vendors with body: " + jsonInputString);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                System.out.println("Response code from /vendors: " + responseCode);
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String response = br.lines().collect(Collectors.joining());
                        System.out.println("Response from /vendors: " + response);
                        return gson.fromJson(response, new TypeToken<List<RestaurantDTO>>(){}.getType());
                    }
                } else {
                    throw new RuntimeException("Failed to fetch restaurants: HTTP " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching restaurants: " + e.getMessage());
            }
        });
    }
    public CompletableFuture<MenuResponseDTO> getMenusAndItems(String token, UUID restaurantId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(BASE_URL + "/vendors/" + restaurantId + "/items");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                int responseCode = conn.getResponseCode();
                System.out.println("Response code from /vendors/" + restaurantId + "/items: " + responseCode);
                if (responseCode == 200) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String response = br.lines().collect(Collectors.joining());
                        System.out.println("Response from /vendors/" + restaurantId + "/items: " + response);
                        return gson.fromJson(response, MenuResponseDTO.class);
                    }





                } else if (responseCode == 404) {
                    throw new RuntimeException("Vendor not found");
                } else {
                    throw new RuntimeException("Failed to fetch menus and items: HTTP " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error fetching menus and items: " + e.getMessage());
            }
        });
    }
    public CompletableFuture<List<FoodDTO>> searchItems(String token, Map<String, Object> filters) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        String jsonBody = gson.toJson(filters);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/items"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), new TypeToken<List<FoodDTO>>(){}.getType());
                    } else {
                        System.out.println("Error fetching items: HTTP " + response.statusCode());
                        return null;
                    }
                });
    }
    public CompletableFuture<String> addFavorite(String token, UUID restaurantId) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/favorites/" + restaurantId))
                .header("Authorization", authHeader)
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                        return json.get("message").getAsString();
                    } else {
                        throw new RuntimeException("Failed to add favorite: " + response.body());
                    }
                });
    }

    public CompletableFuture<String> removeFavorite(String token, UUID restaurantId) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/favorites/" + restaurantId))
                .header("Authorization", authHeader)
                .DELETE()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                        return json.get("message").getAsString();
                    } else {
                        throw new RuntimeException("Failed to remove favorite: " + response.body());
                    }
                });
    }

    public CompletableFuture<List<RestaurantDTO>> getFavorites(String token) {
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/favorites/"))
                .header("Authorization", authHeader)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Response status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), new TypeToken<List<RestaurantDTO>>(){}.getType());
                    } else {
                        String errorMessage = "Failed to get favorites: Status " + response.statusCode() + ", Body: " + response.body();
                        System.err.println(errorMessage);
                        throw new RuntimeException(errorMessage);
                    }
                });
    }
}