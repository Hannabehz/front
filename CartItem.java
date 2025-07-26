package model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

public class CartItem {
    @SerializedName("item_id")
    private UUID itemId;
    @SerializedName("restaurant_id")
    private UUID restaurantId;
    @SerializedName("name")
    private String name;
    @SerializedName("description")
    private String description;
    @SerializedName("price")
    private Double price;
    @SerializedName("quantity")
    private Integer quantity;
    @SerializedName("categories")
    private List<String> categories;
    @SerializedName("supply")
    private Integer supply;
    @SerializedName("image_base64")
    private String imageBase64;

    // سازنده پیش‌فرض برای Gson
    public CartItem() {
    }

    public CartItem(UUID itemId, UUID restaurantId, String name, String description, double price, int quantity, List<String> categories, int supply, String imageBase64) {
        this.itemId = itemId;
        this.restaurantId = restaurantId; // تغییر از vendorId به restaurantId
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.categories = categories;
        this.supply = supply;
        this.imageBase64 = imageBase64;
    }

    // Getters و Setters
    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public UUID getRestaurantId() { // تغییر از getVendorId به getRestaurantId
        return restaurantId;
    }

    public void setRestaurantId(UUID restaurantId) { // تغییر از setVendorId به setRestaurantId
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public Integer getSupply() {
        return supply;
    }

    public void setSupply(Integer supply) {
        this.supply = supply;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}