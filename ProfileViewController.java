package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import service.UserService;

import java.io.File;

import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.io.ByteArrayInputStream;

import java.util.concurrent.CompletableFuture;




import com.google.gson.Gson;

import model.BankInfoDTO;
import model.UserDTO;


public class ProfileViewController {
    @FXML private TextField fullNameField;
    @FXML private TextField passwordField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField bankNameField;
    @FXML private TextField accountNumberField;
    @FXML private TextField addressField;
    @FXML private ChoiceBox<String> roleChoice;
    @FXML private ImageView profileImageView;
    @FXML private Button uploadImageButton;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;

    private final UserService userService = new UserService();
    private String token;
    private String profileImageBase64; // برای ذخیره تصویر Base64
    private Map<String, Object> profileData;
    private Stage stage;
    private Scene mainScene; // صحنه اصلی
    private Scene displayProfileScene;
    public void setToken(String token) {
        this.token = token;
    }
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setMainScene(Scene mainScene) {
        this.mainScene = mainScene;
    }
    public void setProfileData(Map<String, Object> profileData) {
        this.profileData = profileData;
        populateFields();
    }

    private void populateFields() {
        if (profileData == null) {
            statusLabel.setText("خطا: داده‌های پروفایل دریافت نشد!");
            return;
        }

        // پر کردن فیلدها با داده‌های پروفایل
        fullNameField.setText((String) profileData.getOrDefault("fullName", ""));
        phoneField.setText((String) profileData.getOrDefault("phone", ""));
        emailField.setText((String) profileData.getOrDefault("email", ""));
        addressField.setText((String) profileData.getOrDefault("address", ""));
        passwordField.setText(""); // رمز عبور خالی، برای امنیت
        roleChoice.getItems().addAll("buyer", "seller", "courier");
        roleChoice.setValue((String) profileData.getOrDefault("role", "buyer"));

        // اطلاعات بانکی
        Map<String, Object> bankInfo = (Map<String, Object>) profileData.get("bankInfo");
        if (bankInfo != null) {
            bankNameField.setText((String) bankInfo.getOrDefault("bankName", ""));
            accountNumberField.setText((String) bankInfo.getOrDefault("accountNumber", ""));
        }

        // نمایش تصویر پروفایل
        profileImageBase64 = (String) profileData.getOrDefault("profileImageBase64", null);
        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(profileImageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                profileImageView.setImage(image);
            } catch (IllegalArgumentException e) {
                statusLabel.setText("خطا در بارگذاری تصویر پروفایل!");
            }
        }
    }

    @FXML
    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        java.io.File selectedFile = fileChooser.showOpenDialog(saveButton.getScene().getWindow());
        if (selectedFile != null) {
            try {
                byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                profileImageBase64 = Base64.getEncoder().encodeToString(fileContent);
                Image image = new Image(new ByteArrayInputStream(fileContent));
                profileImageView.setImage(image);
                statusLabel.setText("تصویر انتخاب شد: " + selectedFile.getName());
            } catch (Exception e) {
                statusLabel.setText("خطا در بارگذاری تصویر: " + e.getMessage());
            }
        }
    }

    @FXML
    private void saveChanges() {
        String fullName = fullNameField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = passwordField.getText().trim();
        String email = emailField.getText().trim();
        String address = addressField.getText().trim();
        String role = roleChoice.getValue();
        String bankName = bankNameField.getText().trim();
        String accountNumber = accountNumberField.getText().trim();

       /* // اعتبارسنجی فیلدهای اجباری
        if (fullName.isEmpty() || phone.isEmpty() || role == null || address.isEmpty()) {
            statusLabel.setText("لطفاً تمام فیلدهای اجباری را پر کنید!");
            return;
        }*/

        // آماده‌سازی داده‌ها برای ارسال
        UserDTO userDTO = new UserDTO();
        userDTO.setFullName(fullName);
        userDTO.setPhone(phone);
        if (!password.isEmpty()) {
            userDTO.setPassword(password);
        }
        userDTO.setEmail(email.isEmpty() ? null : email);
        userDTO.setRole(role);
        userDTO.setPassword(password.isEmpty() ? null : password);
        userDTO.setAddress(address);
        userDTO.setProfileImageBase64(profileImageBase64);
        if (!bankName.isEmpty() && !accountNumber.isEmpty()) {
            userDTO.setBankInfo(new BankInfoDTO(bankName, accountNumber));
        } else {
            userDTO.setBankInfo(null);
        }

        Map<String, Object> profileDataToSend = new Gson().fromJson(new Gson().toJson(userDTO), Map.class);

        CompletableFuture<Map<String, Object>> future = userService.updateProfile(token, profileDataToSend);
        future.thenAccept(response -> Platform.runLater(() -> {
            if (response != null && response.containsKey("status")) {
                // تبدیل ایمن مقدار status به int
                int status;
                Object statusObj = response.get("status");
                if (statusObj instanceof Double) {
                    status = ((Double) statusObj).intValue();
                } else if (statusObj instanceof Integer) {
                    status = (Integer) statusObj;
                } else {
                    statusLabel.setText("نوع داده status نامعتبر است!");
                    return;
                }

                if (status == 200 && response.containsKey("user")) {
                    statusLabel.setText("پروفایل با موفقیت به‌روزرسانی شد!");
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/demo1/DisplayProfile.fxml"));
                        Parent root = loader.load();
                        DisplayProfileController controller = loader.getController();
                        controller.setToken(token);
                        controller.setProfileData((Map<String, Object>) response.get("user"));
                        Stage stage = (Stage) saveButton.getScene().getWindow();
                        stage.setScene(new Scene(root, 600, 400));
                        stage.setTitle("نمایش پروفایل");
                        stage.show();
                    } catch (Exception e) {
                        statusLabel.setText("خطا در بازگشت به صفحه پروفایل: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    statusLabel.setText(response.getOrDefault("message", "خطا در به‌روزرسانی پروفایل").toString());
                }
            }
        })).exceptionally(throwable -> {
            Platform.runLater(() -> statusLabel.setText("خطا: " + throwable.getMessage()));
            return null;
        });
    }
    @FXML
    private void goBack() {
        if (stage != null && displayProfileScene != null) {
            stage.setScene(displayProfileScene); // بازگشت به صفحه نمایش پروفایل
            stage.setTitle("نمایش پروفایل");
        } else if (stage != null && mainScene != null) {
            stage.setScene(mainScene); // بازگشت به صفحه اصلی
            stage.setTitle("صفحه اصلی");
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("خطا");
            alert.setHeaderText(null);
            alert.setContentText("نمی‌توان به صفحه قبلی بازگشت!");
            alert.showAndWait();
        }
    }

    public void setDisplayProfileScene(Scene displayProfileScene) {
        this.displayProfileScene = displayProfileScene;
    }
}