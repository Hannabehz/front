package view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.UserDTO;
import service.UserService;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class SignUpView {
    private final UserService authService = new UserService();
    private final Stage stage;

    public SignUpView(Stage stage) {
        this.stage = stage;
        setupUI();
    }

    private void setupUI() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // تنظیم استایل پس‌زمینه سبز
        vbox.setStyle("-fx-background-color: #4CAF50;");

        // فیلدهای اجباری
        Label fullNameLabel = new Label("Full Name:");
        TextField fullNameField = new TextField();
        fullNameField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label phoneLabel = new Label("Phone:");
        TextField phoneField = new TextField();
        phoneField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label passwordLabel = new Label("Password:");
        PasswordField passwordField = new PasswordField();
        passwordField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label roleLabel = new Label("Role:");
        ChoiceBox<String> roleChoice = new ChoiceBox<>();
        roleChoice.getItems().addAll("buyer", "seller", "courier");
        roleChoice.setValue("buyer");
        Label addressLabel = new Label("Address:");
        TextField addressField = new TextField();
        addressField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");

        // فیلدهای اختیاری
        Label emailLabel = new Label("Email(Optional):");
        TextField emailField = new TextField();
        emailField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label bankNameLabel = new Label("Bank Name (Optional):");
        TextField bankNameField = new TextField();
        bankNameField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label accountNumberLabel = new Label("Account Number (Optional):");
        TextField accountNumberField = new TextField();
        accountNumberField.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label profileImageLabel = new Label("Profile Image (Optional):");
        Button uploadImageButton = new Button("Upload Image");
        uploadImageButton.setStyle("-fx-background-color: #D3D3D3; -fx-text-fill: black;");
        Label imagePreviewLabel = new Label("No image selected");
        imagePreviewLabel.setStyle("-fx-text-fill: white;");
        String[] profileImageBase64 = {null}; // آرایه برای دسترسی در لامبدا

        uploadImageButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                try {
                    byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                    profileImageBase64[0] = Base64.getEncoder().encodeToString(fileContent);
                    imagePreviewLabel.setText("Image selected: " + selectedFile.getName());
                } catch (Exception ex) {
                    imagePreviewLabel.setText("Error loading image: " + ex.getMessage());
                }
            }
        });
        // استیکرهای غذا
        ImageView sticker1 = new ImageView(new Image("file:src/main/resources/images/img.png", 200, 200, true, true));
        //ImageView sticker2 = new ImageView(new Image("file:src/main/resources/images/food2.jpg", 100, 100, true, true));
        sticker1.setLayoutY(10); // ارتفاع ثابت بالا نگه داشته شده
                HBox stickerBox = new HBox();
        stickerBox.setAlignment(Pos.CENTER); // وسط‌چین افقی
        stickerBox.getChildren().add(sticker1);
        vbox.getChildren().add(0, stickerBox); // اضافه کردن به اول vbox
// موقعیت استیکر دوم (مثال)
        /*sticker2.setLayoutX(200);
        sticker2.setLayoutY(200);*/

// به‌روزرسانی موقعیت با تغییر عرض صحنه
       /* scene.widthProperty().addListener((obs, oldVal, newVal) -> {
            sticker1.setLayoutX((newVal.doubleValue() - sticker1.getFitWidth()) / 2);
        });*/
        //vbox.getChildren().addAll(sticker1);

        // دکمه و پیام
        Button signUpButton = new Button("Sign Up");
        signUpButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: white;");

        // رفتن به فیلد بعدی با Enter
        TextField[] fields = {fullNameField, phoneField, passwordField, addressField, emailField, bankNameField, accountNumberField};
        for (int i = 0; i < fields.length; i++) {
            final int index = i;
            fields[i].setOnKeyPressed(event -> {
                if (event.getCode().toString().equals("ENTER")) {
                    if (index < fields.length - 1) {
                        fields[index + 1].requestFocus();
                    } else {
                        signUpButton.requestFocus();
                    }
                }
            });
        }

        // دکمه ثبت‌نام
        signUpButton.setOnAction(e -> {
            String fullName = fullNameField.getText().trim();
            String phone = phoneField.getText().trim();
            String password = passwordField.getText().trim();
            String role = roleChoice.getValue();
            String address = addressField.getText().trim();
            String email = emailField.getText().trim();
            String bankName = bankNameField.getText().trim();
            String accountNumber = accountNumberField.getText().trim();

            if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty() || role == null || address.isEmpty()) {
                messageLabel.setText("Please fill all required fields!");
                return;
            }

            UserDTO user = new UserDTO(fullName, phone, password, role, address, email.isEmpty() ? null : email,
                    bankName.isEmpty() ? null : bankName, accountNumber.isEmpty() ? null : accountNumber);
            user.setProfileImageBase64(profileImageBase64[0]);
            System.out.println("Sending user: " + fullName + ", " + phone + ", " + password + ", " + role + ", " + address +
                    ", email: " + email + ", bankName: " + bankName + ", accountNumber: " + accountNumber);

            CompletableFuture<Map<String, Object>> future = authService.signUp(user);
            future.thenAccept(response -> {
                Platform.runLater(() -> {
                    System.out.println("Response from server: " + response);
                    if (response != null) {
                        if (response.containsKey("error")) {
                            messageLabel.setText("Error: " + response.get("error"));
                        } else if (response.containsKey("message")) {
                            String message = (String) response.get("message");
                            if (response.containsKey("status") && response.get("status") instanceof Integer && (int) response.get("status") == 200) {
                                String userId = response.containsKey("user_id") ? (String) response.get("user_id") : "N/A";
                                String token = response.containsKey("token") ? (String) response.get("token") : "N/A";
                                messageLabel.setText("Success: " + message + " (User ID: " + userId + ", Token: " + token + ")");
                            } else {
                                messageLabel.setText(message);
                            }
                        } else {
                            messageLabel.setText("Unexpected response from server! Response: " + response.toString());
                        }
                    } else {
                        messageLabel.setText("No response from server!");
                    }
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    System.out.println("Exception: " + throwable.getMessage());
                    messageLabel.setText(throwable.getMessage());
                });
                return null;
            });
        });

        Hyperlink loginLink = new Hyperlink("Already have an account? Login");
        loginLink.setStyle("-fx-text-fill: white; -fx-underline: true;");
        loginLink.setOnAction(e -> {
            LoginView loginView = new LoginView(stage); // فرض می‌کنیم LoginView تعریف شده
        });

        // اضافه کردن به VBox
        vbox.getChildren().addAll(fullNameLabel, fullNameField, phoneLabel, phoneField, passwordLabel, passwordField,
                roleLabel, roleChoice, addressLabel, addressField, emailLabel, emailField,
                bankNameLabel, bankNameField, accountNumberLabel, accountNumberField,
                profileImageLabel, uploadImageButton, imagePreviewLabel, signUpButton, loginLink, messageLabel);
        Scene scene = new Scene(vbox, 900, 900);
        stage.setScene(scene);
        stage.setTitle("Sign Up");
        stage.show();
    }
}