package service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.Order;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CourierService {
    private static final String BASE_URL = "http://localhost:8080";
    private final Gson gson;
    private String token;
    public CourierService() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            @Override
            public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                return LocalDateTime.parse(json.getAsString(), formatter);
            }
        });
        gson = gsonBuilder.create();
    }
    public CompletableFuture<List<Order>> getAvailableDeliveries(String token) {
        System.out.println("Sending token to /delivery/available: " + token);
        HttpClient client = HttpClient.newHttpClient();
        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/delivery/available"))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    System.out.println("Raw response from /delivery/available: " + body);
                    if (response.statusCode() == 200) {
                        // تبدیل JSON به لیست model.Order
                        List<Order> orders = gson.fromJson(body, new TypeToken<List<model.Order>>() {}.getType());
                        return orders;
                    } else {
                        Map<String, Object> result = gson.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType());
                        throw new RuntimeException(result.getOrDefault("error", "Failed to fetch available deliveries").toString());
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Delivery fetch error: " + throwable.getMessage());
                    throw new RuntimeException("Internal server error: " + throwable.getMessage());
                });
    }
    public CompletableFuture<List<Order>> getDeliveryHistory(String token) {
        System.out.println("Attempting to fetch delivery history..."); // لاگ دیباگ
        HttpClient client = HttpClient.newHttpClient();
        String url = BASE_URL + "/deliveries/history";
        System.out.println("Request URL: " + url); // تأیید URL

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("Response status: " + response.statusCode()); // لاگ وضعیت پاسخ
                    if (response.statusCode() == 200) {
                        return gson.fromJson(response.body(), new TypeToken<List<Order>>(){}.getType());
                    } else {
                        throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
                    }
                });
    }
    // در CourierService.java
    public CompletableFuture<Void> updateOrderStatus(String orderId, String newStatus, String token) {
        HttpClient client = HttpClient.newHttpClient();
        Map<String, String> body = new HashMap<>();
        body.put("deliveryStatus", newStatus);
        String requestBody = gson.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/deliveries/" + orderId))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("Error: " + response.statusCode() + " - " + response.body());
                    }
                });
    }
}
