package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Base64;

public class RatingDialogController {
    @FXML private Slider ratingSlider;
    @FXML private TextArea commentTextArea;
    @FXML private TextField imagePathField;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private String token;
    private String orderId;
    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @FXML
    private void uploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            imagePathField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void submitRating() {
        int rating = (int) ratingSlider.getValue();
        String comment = commentTextArea.getText().trim();
        String imageBase64 = null;

        if (comment.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "خطا", "لطفاً نظر خود را وارد کنید.");
            return;
        }

        if (!imagePathField.getText().isEmpty()) {
            try {
                File file = new File(imagePathField.getText());
                byte[] fileContent = Files.readAllBytes(file.toPath());
                imageBase64 = Base64.getEncoder().encodeToString(fileContent);
                System.out.println("ImageBase64 length: " + (imageBase64 != null ? imageBase64.length() : 0));
            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "خطا", "خطا در بارگذاری تصویر: " + e.getMessage());
                return;
            }
        }

        String jsonBody = String.format("{\"orderId\":\"%s\",\"rating\":%d,\"comment\":\"%s\",\"imageBase64\":%s}",
                orderId, rating, comment, imageBase64 != null ? "\"" + imageBase64 + "\"" : "null");

        System.out.println("Sending request to /ratings with body: " + jsonBody);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/ratings"))
                .header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    int statusCode = response.statusCode();
                    String responseBody = response.body();
                    System.out.println("Response from /ratings - Status: " + statusCode + ", Body: " + responseBody);
                    if (statusCode == 200) {
                        Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "موفقیت", "نظر با موفقیت ثبت شد.");
                            stage.close();
                        });
                    } else {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ثبت نظر: " + responseBody));
                    }
                })
                .exceptionally(throwable -> {
                    System.err.println("Exception in submitRating: " + throwable.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "خطا", "خطا در ارتباط با سرور: " + throwable.getMessage()));
                    return null;
                });
    }

    @FXML
    private void cancel() {
        stage.close();
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