package com.example.weather;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherApp extends Application {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("Meteo App");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        TextField cityField = new TextField();
        cityField.setPromptText("Inserisci una città, Roma, Milano, ...");
        cityField.setStyle(
            "-fx-font-size: 15px; -fx-padding: 10 14; -fx-background-radius: 24; " +
            "-fx-border-radius: 24; -fx-background-color: white; -fx-prompt-text-fill: #888;"
        );
        cityField.setMaxWidth(320);

        Button searchBtn = new Button("CERCA");
        searchBtn.setStyle(
            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: #3b82f6; -fx-background-radius: 24; -fx-padding: 10 32; " +
            "-fx-cursor: hand;"
        );
        searchBtn.setOnMouseEntered(e ->
            searchBtn.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-background-color: #2563eb; -fx-background-radius: 24; -fx-padding: 10 32; " +
                "-fx-cursor: hand;"
            )
        );
        searchBtn.setOnMouseExited(e ->
            searchBtn.setStyle(
                "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; " +
                "-fx-background-color: #3b82f6; -fx-background-radius: 24; -fx-padding: 10 32; " +
                "-fx-cursor: hand;"
            )
        );

        Label resultLabel = new Label();
        resultLabel.setFont(Font.font("Segoe UI", FontWeight.LIGHT, 40));
        resultLabel.setTextFill(Color.WHITE);
        resultLabel.setWrapText(true);
        resultLabel.setAlignment(Pos.CENTER);

        searchBtn.setOnAction(e -> {
            String city = cityField.getText().trim();
            if (!city.isEmpty()) {
                resultLabel.setText("Caricamento...");
                fetchWeather(city, resultLabel);
            }
        });

        cityField.setOnAction(e -> searchBtn.fire());

        VBox root = new VBox(20, title, cityField, searchBtn, resultLabel);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 30, 50, 30));
        root.setBackground(new Background(new BackgroundFill(
            new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#1e3c72")),
                new Stop(1, Color.web("#2a5298"))),
            CornerRadii.EMPTY, Insets.EMPTY
        )));

        primaryStage.setScene(new Scene(root, 480, 460));
        primaryStage.setTitle("Meteo App");
        primaryStage.show();
    }

    private void fetchWeather(String city, Label resultLabel) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=it&format=json";

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                HttpRequest geoReq = HttpRequest.newBuilder()
                        .uri(URI.create(geoUrl))
                        .GET()
                        .build();
                HttpResponse<String> geoResp = client.send(geoReq, HttpResponse.BodyHandlers.ofString());
                if (geoResp.statusCode() != 200) throw new RuntimeException("Geocoding error: " + geoResp.body());
                JsonNode geoData = mapper.readTree(geoResp.body());
                JsonNode results = geoData.get("results");
                if (results == null || !results.isArray() || results.isEmpty())
                    throw new RuntimeException("Località non trovata: " + city);
                JsonNode first = results.get(0);
                double lat = first.get("latitude").asDouble();
                double lon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,weather_code",
                    lat, lon
                );
                HttpRequest weatherReq = HttpRequest.newBuilder()
                        .uri(URI.create(weatherUrl))
                        .GET()
                        .build();
                HttpResponse<String> weatherResp = client.send(weatherReq, HttpResponse.BodyHandlers.ofString());
                if (weatherResp.statusCode() != 200) throw new RuntimeException("Weather error: " + weatherResp.body());
                JsonNode weatherData = mapper.readTree(weatherResp.body());
                double temp = weatherData.at("/current/temperature_2m").asDouble();
                int code = weatherData.at("/current/weather_code").asInt();
                return String.format("%.1f °C\n%s", temp, weatherDescription(code));
            }
        };

        task.setOnSucceeded(ev -> resultLabel.setText(task.getValue()));
        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Errore: " + ex.getMessage());
            a.showAndWait();
            resultLabel.setText("");
        });

        new Thread(task).start();
    }

    private String weatherDescription(int code) {
        if (code == 0) return "\u2600\uFE0F Sereno";
        if (code <= 3) return "\u26C5 Nuvoloso";
        if (code <= 48) return "\uD83C\uDF2B\uFE0F Nebbia";
        if (code <= 57) return "\uD83C\uDF26\uFE0F\uFE0F Pioggerella";
        if (code <= 67) return "\uD83C\uDF27\uFE0F Pioggia";
        if (code <= 77) return "\u2744\uFE0F Neve";
        if (code <= 82) return "\uD83C\uDF28\uFE0F Rovesci";
        if (code <= 86) return "\uD83C\uDF28\uFE0F Nevischio";
        return "\u26A1 Temporale";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
