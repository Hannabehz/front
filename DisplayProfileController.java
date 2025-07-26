package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import service.UserService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Base64;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Map;

public class DisplayProfileController {
    @FXML private ImageView profileImageView;
    @FXML private Label profileInfoLabel;
    @FXML private Button editButton;

    private String token;
    private Map<String, Object> profileData;

    public void setProfileData(Map<String, Object> profileData) {
        this.profileData = profileData;
        displayProfile();
    }

    public void setToken(String token) {
        this.token = token;
    }

    private void displayProfile() {
        if (profileData == null) {
            profileInfoLabel.setText("خطا: داده‌های پروفایل دریافت نشد!");
            return;
        }

        String fullName = (String) profileData.getOrDefault("fullName", "N/A");
        String phone = (String) profileData.getOrDefault("phone", "N/A");
        String email = (String) profileData.getOrDefault("email", "N/A");
        String role = (String) profileData.getOrDefault("role", "N/A");
        String address = (String) profileData.getOrDefault("address", "N/A");
        String profileImageBase64 = (String) profileData.getOrDefault("profileImageBase64", null);

        StringBuilder profileInfo = new StringBuilder();
        profileInfo.append("نام: ").append(fullName).append("\n")
                .append("تلفن: ").append(phone).append("\n")
                .append("ایمیل: ").append(email != null ? email : "وارد نشده").append("\n")
                .append("نقش: ").append(role).append("\n")
                .append("آدرس: ").append(address).append("\n");

        Map<String, Object> bankInfo = (Map<String, Object>) profileData.get("bankInfo");
        if (bankInfo != null) {
            String bankName = (String) bankInfo.getOrDefault("bankName", "N/A");
            String accountNumber = (String) bankInfo.getOrDefault("accountNumber", "N/A");
            profileInfo.append("نام بانک: ").append(bankName).append("\n")
                    .append("شماره حساب: ").append(accountNumber);
        } else {
            profileInfo.append("اطلاعات بانکی: وارد نشده");
        }

        profileInfoLabel.setText(profileInfo.toString());

        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(profileImageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                profileImageView.setImage(image);
            } catch (IllegalArgumentException e) {
                profileInfoLabel.setText(profileInfoLabel.getText() + "\nخطا در بارگذاری تصویر پروفایل!");
            }
        } else {
            profileImageView.setImage(null); // یا تصویر پیش‌فرض
        }
    }

    @FXML
    private void editProfile() {
        try {
            // بررسی مسیر فایل FXML
            String fxmlPath = "/org/example/demo1/editProfileView.fxml";
            if (getClass().getResource(fxmlPath) == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("خطا");
                alert.setHeaderText(null);
                alert.setContentText("فایل ProfileView.fxmledit پیدا نشد! مسیر: " + fxmlPath);
                alert.showAndWait();
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            ProfileViewController controller = loader.getController();
            controller.setToken(token);
            controller.setProfileData(profileData);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 600, 600));
            stage.setTitle("ویرایش پروفایل");
            stage.show();

            // بستن پنجره فعلی (اختیاری)
            Stage currentStage = (Stage) editButton.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطا");
            alert.setHeaderText(null);
            alert.setContentText("خطا در بارگذاری صفحه ویرایش: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
}