package com.example.weather;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
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
        TextField cityField = new TextField();
        cityField.setPromptText("Città o località");
        Button searchBtn = new Button("Cerca");
        Label resultLabel = new Label();

        searchBtn.setOnAction(e -> {
            String city = cityField.getText().trim();
            if (!city.isEmpty()) {
                fetchWeather(city, resultLabel);
            }
        });

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Località:"), 0, 0);
        grid.add(cityField, 1, 0);
        grid.add(searchBtn, 2, 0);
        grid.add(resultLabel, 0, 1, 3, 1);

        primaryStage.setScene(new Scene(grid, 450, 150));
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
                return String.format("%.1f °C (%s)", temp, weatherDescription(code));
            }
        };

        task.setOnSucceeded(ev -> resultLabel.setText(task.getValue()));
        task.setOnFailed(ev -> {
            Throwable ex = task.getException();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setContentText("Errore: " + ex.getMessage());
            a.showAndWait();
        });

        new Thread(task).start();
    }

    private String weatherDescription(int code) {
        if (code == 0) return "Sereno";
        if (code <= 3) return "Nuvoloso";
        if (code <= 48) return "Nebbia";
        if (code <= 57) return "Pioggerella";
        if (code <= 67) return "Pioggia";
        if (code <= 77) return "Neve";
        if (code <= 82) return "Rovesci";
        if (code <= 86) return "Nevischio";
        return "Temporale";
    }

    public static void main(String[] args) {
        launch(args);
    }
}
