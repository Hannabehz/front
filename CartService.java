package service;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import model.CartItem;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CartService {
    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    public CompletableFuture<Map<String, Object>> getCart(String token) {
        if (token == null || token.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of(
                    "status", 400,
                    "message", "Token is required"
            ));
        }

        System.out.println("Sending token to /auth/cart: " + token);
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/cart"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Raw response from /auth/cart: " + body);
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    if (response.statusCode() == 200) {
                        Map<String, Object> cartResult = new HashMap<>();
                        cartResult.put("status", 200);
                        cartResult.put("cart", result);
                        return cartResult;
                    } else {
                        return Map.of(
                                "status", response.statusCode(),
                                "message", result != null ? result.getOrDefault("message", "Cart fetch failed") : "Cart fetch failed"
                        );
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Cart fetch error: " + throwable.getMessage());
                    return Map.of(
                            "status", 500,
                            "message", "Internal server error: " + throwable.getMessage()
                    );
                });
    }

    public CompletableFuture<List<model.CartItem>> getCartItems(String token) {
        if (token == null || token.trim().isEmpty()) {
            System.out.println("Token is null or empty");
            return CompletableFuture.completedFuture(new ArrayList<model.CartItem>());
        }

        System.out.println("Sending request to /auth/cart with token: " + token);
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/cart"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Received response from /auth/cart, status: " + response.statusCode() + ", body: " + response.body());
                    String body = response.body();
                    if (response.statusCode() == 200) {
                        Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
                        if (items != null) {
                            List<model.CartItem> cartItems = items.stream().map(item -> {
                                model.CartItem cartItem = new model.CartItem();
                                cartItem.setItemId(item.containsKey("item_id") ? UUID.fromString((String) item.get("item_id")) : null);
                                cartItem.setRestaurantId(item.containsKey("restaurant_id") ? UUID.fromString((String) item.get("restaurant_id")) : null); // تغییر از vendor_id به restaurant_id
                                cartItem.setName((String) item.get("name"));
                                cartItem.setDescription((String) item.get("description"));
                                cartItem.setPrice(item.containsKey("price") ? ((Number) item.get("price")).doubleValue() : 0.0);
                                cartItem.setQuantity(item.containsKey("quantity") ? ((Number) item.get("quantity")).intValue() : 0);
                                cartItem.setCategories(item.containsKey("categories") ? (List<String>) item.get("categories") : new ArrayList<>());
                                cartItem.setSupply(item.containsKey("supply") ? ((Number) item.get("supply")).intValue() : 0);
                                cartItem.setImageBase64((String) item.get("image_base64"));
                                return cartItem;
                            }).collect(Collectors.toList());
                            return cartItems;
                        }
                        return new ArrayList<model.CartItem>();
                    } else {
                        System.err.println("Failed to fetch cart items, status: " + response.statusCode() + ", body: " + body);
                        return new ArrayList<model.CartItem>();
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Get cart items error: " + throwable.getMessage());
                    return new ArrayList<model.CartItem>();
                });
    }

    public CompletableFuture<Map<String, Object>> addToCart(String token, UUID itemId, int quantity, UUID restaurantId) { // اضافه کردن restaurantId
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("item_id", itemId.toString());
        requestBody.put("quantity", quantity);
        requestBody.put("restaurant_id", restaurantId.toString()); // اضافه کردن restaurant_id
        String jsonBody = gson.toJson(requestBody);
        System.out.println("Request body to /auth/cart (add): " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/cart"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Raw response from /auth/cart (add): " + body);
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    return Map.of(
                            "status", response.statusCode(),
                            "message", result != null ? result.getOrDefault("message", "Add to cart failed") : "Add to cart failed",
                            "data", result != null ? result : new HashMap<>()
                    );
                })
                .exceptionally(throwable -> {
                    System.err.println("Add to cart error: " + throwable.getMessage());
                    return Map.of(
                            "status", 500,
                            "message", "Internal server error: " + throwable.getMessage()
                    );
                });
    }

    public CompletableFuture<Map<String, Object>> removeFromCart(String token, UUID itemId, UUID restaurantId) { // اضافه کردن restaurantId
        if (token == null || token.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of(
                    "status", 400,
                    "message", "Token is required"
            ));
        }
        if (itemId == null) {
            return CompletableFuture.completedFuture(Map.of(
                    "status", 400,
                    "message", "Item ID is required"
            ));
        }
        System.out.println("Sending request to /auth/cart with token: " + token);
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("item_id", itemId.toString());
        requestBody.put("restaurant_id", restaurantId.toString()); // اضافه کردن restaurant_id
        String jsonBody = gson.toJson(requestBody);
        System.out.println("Request body to /auth/cart: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/auth/cart"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Raw response from /auth/cart: " + body);
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    return Map.of(
                            "status", response.statusCode(),
                            "message", result != null ? result.getOrDefault("message", "Remove from cart failed") : "Remove from cart failed",
                            "data", result != null ? result : new HashMap<>()
                    );
                })
                .exceptionally(throwable -> {
                    System.err.println("Remove from cart error: " + throwable.getMessage());
                    return Map.of(
                            "status", 500,
                            "message", "Internal server error: " + throwable.getMessage()
                    );
                });
    }

    public CompletableFuture<Integer> getCartItemQuantity(String token, String itemId) {
        if (token == null || token.trim().isEmpty() || itemId == null || itemId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(0);
        }

        return getCartItems(token).thenApply(items -> {
            return items.stream()
                    .filter(item -> item.getItemId().equals(UUID.fromString(itemId)))
                    .mapToInt(CartItem::getQuantity)
                    .sum();
        });
    }

    public CompletableFuture<Map<String, Object>> topUpWallet(String token, String method, double amount, String bankName, String accountNumber) {
        if (token == null || token.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Token is required"));
        }
        if (method == null || !method.matches("online|card")) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Invalid method"));
        }
        if (amount <= 0) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Invalid amount"));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("method", method);
        requestBody.put("amount", amount);
        if (bankName != null) requestBody.put("bankName", bankName);
        if (accountNumber != null) requestBody.put("accountNumber", accountNumber);

        String jsonBody = gson.toJson(requestBody);
        System.out.println("Request body to /wallet/top-up: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/wallet/top-up"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Response from /wallet/top-up: " + body);
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    return Map.of(
                            "status", response.statusCode(),
                            "message", result != null ? result.getOrDefault("message", "Top-up failed") : "Top-up failed",
                            "data", result != null ? result : new HashMap<>()
                    );
                })
                .exceptionally(throwable -> Map.of(
                        "status", 500,
                        "message", "Internal server error: " + throwable.getMessage()
                ));
    }

    public CompletableFuture<Map<String, Object>> makeOnlinePayment(String token, String orderId, String method, String bankName, String accountNumber) {
        if (token == null || token.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Token is required"));
        }
        if (orderId == null || orderId.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Order ID is required"));
        }
        if (method == null || !method.matches("wallet|paywall")) {
            return CompletableFuture.completedFuture(Map.of("status", 400, "message", "Invalid method"));
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("orderId", orderId);
        requestBody.put("method", method);
        if (bankName != null) requestBody.put("bankName", bankName);
        if (accountNumber != null) requestBody.put("accountNumber", accountNumber);

        String jsonBody = gson.toJson(requestBody);
        System.out.println("Request body to /payment/online: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/payment/online"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Response from /payment/online: " + body);
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    return Map.of(
                            "status", response.statusCode(),
                            "message", result != null ? result.getOrDefault("message", "Payment failed") : "Payment failed",
                            "data", result != null ? result : new HashMap<>()
                    );
                })
                .exceptionally(throwable -> Map.of(
                        "status", 500,
                        "message", "Internal server error: " + throwable.getMessage()
                ));
    }

    public CompletableFuture<Map<String, Object>> submitOrder(String token, Map<String, Object> requestBody) {
        if (token == null || token.trim().isEmpty()) {
            return CompletableFuture.completedFuture(Map.of(
                    "status", 400,
                    "error", "Token is required"
            ));
        }
        System.out.println("Request body to /orders: " + requestBody);
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        // اضافه کردن restaurant_id به هر آیتم
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) requestBody.get("items");
        if (items != null) {
            UUID restaurantId = UUID.fromString((String) requestBody.get("restaurant_id"));
            for (Map<String, Object> item : items) {
                item.put("restaurant_id", restaurantId.toString());
            }
        }
        String jsonBody = gson.toJson(requestBody);
        System.out.println("Request body to /orders: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/orders"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Raw response from /orders: " + body);
                    if (response.statusCode() != 200) {
                        Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                        return Map.of(
                                "status", response.statusCode(),
                                "error", result != null ? result.getOrDefault("error", "Submit order failed") : "Submit order failed",
                                "data", new HashMap<>()
                        );
                    }
                    Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());
                    return Map.of(
                            "status", response.statusCode(),
                            "message", result != null ? result.getOrDefault("message", "Order submitted successfully") : "Order submitted successfully",
                            "data", result != null ? result : new HashMap<>()
                    );
                })
                .exceptionally(throwable -> {
                    System.err.println("Submit order error: " + throwable.getMessage());
                    return Map.of(
                            "status", 500,
                            "error", "Internal server error: " + throwable.getMessage(),
                            "data", new HashMap<>()
                    );
                });
    }
}