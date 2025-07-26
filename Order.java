package model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Order {
    @SerializedName("id")
    private UUID id;

    @SerializedName("deliveryAddress")
    private String deliveryAddress;

    @SerializedName("restaurantName")
    private String restaurantName;

    @SerializedName("items")
    private List<OrderItemDTO> items;

    @SerializedName("payPrice")
    private Integer payPrice;

    @SerializedName("createdAt")
    private LocalDateTime createdAt;

    @SerializedName("status")
    private String status;

    @SerializedName("deliveryStatus")
    private String deliveryStatus;

    // سازنده پیش‌فرض برای Gson
    public Order() {}

    // سازنده با پارامترها
    public Order(UUID id, String deliveryAddress, String restaurantName, List<OrderItemDTO> items,
                 Integer payPrice, LocalDateTime createdAt, String status, String deliveryStatus) {
        this.id = id;
        this.deliveryAddress = deliveryAddress;
        this.restaurantName = restaurantName;
        this.items = items;
        this.payPrice = payPrice;
        this.createdAt = createdAt;
        this.status = status;
        this.deliveryStatus = deliveryStatus;
    }

    // Getters و Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }

    public Integer getPayPrice() { return payPrice; }
    public void setPayPrice(Integer payPrice) { this.payPrice = payPrice; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}