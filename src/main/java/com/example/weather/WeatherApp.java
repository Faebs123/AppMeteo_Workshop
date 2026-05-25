package com.example.weather;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private Label errorLabel;
    private VBox resultCard;
    private Label loadingLabel;
    private StackPane cardContainer;

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("Meteo App");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        TextField cityField = new TextField();
        cityField.setPromptText("Roma, Milano, Tokyo...");
        cityField.setStyle(
            "-fx-font-size: 14px; -fx-padding: 10 14; -fx-background-radius: 22; " +
            "-fx-border-radius: 22; -fx-background-color: white; -fx-prompt-text-fill: #999;"
        );
        cityField.setMaxWidth(300);

        Button searchBtn = new Button("CERCA");
        searchBtn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: #3b82f6; -fx-background-radius: 22; -fx-padding: 10 28; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 4);"
        );
        searchBtn.setOnMouseEntered(e -> searchBtn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: #2563eb; -fx-background-radius: 22; -fx-padding: 10 28; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(37,99,235,0.5), 12, 0, 0, 6);"
        ));
        searchBtn.setOnMouseExited(e -> searchBtn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: #3b82f6; -fx-background-radius: 22; -fx-padding: 10 28; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 4);"
        ));

        HBox searchRow = new HBox(10, cityField, searchBtn);
        searchRow.setAlignment(Pos.CENTER);

        loadingLabel = new Label("Caricamento...");
        loadingLabel.setFont(Font.font("System", 16));
        loadingLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));
        loadingLabel.setVisible(false);

        errorLabel = new Label();
        errorLabel.setFont(Font.font("System", 13));
        errorLabel.setTextFill(Color.rgb(255, 200, 200, 0.95));
        errorLabel.setWrapText(true);
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(380);
        errorLabel.setVisible(false);

        resultCard = createResultCard();
        resultCard.setVisible(false);

        cardContainer = new StackPane(resultCard);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setMaxWidth(400);

        searchBtn.setOnAction(e -> search(cityField));
        cityField.setOnAction(e -> searchBtn.fire());

        VBox root = new VBox(16, title, searchRow, errorLabel, loadingLabel, cardContainer);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30, 20, 30, 20));
        root.setBackground(new Background(new BackgroundFill(
            new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#0f2027")),
                new Stop(0.5, Color.web("#203a43")),
                new Stop(1, Color.web("#2c5364"))),
            CornerRadii.EMPTY, Insets.EMPTY
        )));

        primaryStage.setScene(new Scene(root, 500, 520));
        primaryStage.setTitle("Meteo App");
        primaryStage.show();
    }

    private VBox createResultCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setBackground(new Background(new BackgroundFill(
            Color.rgb(255, 255, 255, 0.12), new CornerRadii(20), Insets.EMPTY
        )));
        card.setMaxWidth(400);
        card.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.3)));
        return card;
    }

    private void search(TextField cityField) {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;

        errorLabel.setVisible(false);
        resultCard.setVisible(false);
        loadingLabel.setVisible(true);

        fetchWeather(city);
    }

    private void fetchWeather(String city) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                HttpRequest geoReq = HttpRequest.newBuilder()
                        .uri(URI.create(geoUrl)).GET().build();
                HttpResponse<String> geoResp = client.send(geoReq, HttpResponse.BodyHandlers.ofString());
                if (geoResp.statusCode() != 200) throw new RuntimeException("Errore geocoding: " + geoResp.body());
                JsonNode geoData = mapper.readTree(geoResp.body());
                JsonNode results = geoData.get("results");
                if (results == null || !results.isArray() || results.isEmpty())
                    throw new RuntimeException("Località \"" + city + "\" non trovata.");
                JsonNode first = results.get(0);
                String cityName = first.get("name").asText();
                String country = first.has("country_code") ? first.get("country_code").asText().toUpperCase() : "";
                double lat = first.get("latitude").asDouble();
                double lon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    lat, lon);
                HttpRequest weatherReq = HttpRequest.newBuilder()
                        .uri(URI.create(weatherUrl)).GET().build();
                HttpResponse<String> weatherResp = client.send(weatherReq, HttpResponse.BodyHandlers.ofString());
                if (weatherResp.statusCode() != 200) throw new RuntimeException("Errore meteo: " + weatherResp.body());
                JsonNode w = mapper.readTree(weatherResp.body()).at("/current");

                double temp = w.get("temperature_2m").asDouble();
                double feels = w.get("apparent_temperature").asDouble();
                int humidity = w.get("relative_humidity_2m").asInt();
                double wind = w.get("wind_speed_10m").asDouble();
                int code = w.get("weather_code").asInt();

                WeatherData data = new WeatherData(cityName, country, temp, feels, humidity, wind, code);
                javafx.application.Platform.runLater(() -> showResult(data));
                return null;
            }

            @Override
            protected void failed() {
                Throwable ex = getException();
                loadingLabel.setVisible(false);
                errorLabel.setText(ex.getMessage());
                errorLabel.setVisible(true);
            }
        };

        new Thread(task).start();
    }

    private void showResult(WeatherData data) {
        loadingLabel.setVisible(false);
        resultCard.getChildren().clear();

        Label cityLabel = new Label(data.city + (data.country.isEmpty() ? "" : ", " + data.country));
        cityLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
        cityLabel.setTextFill(Color.rgb(255, 255, 255, 0.85));
        cityLabel.setAlignment(Pos.CENTER);

        Label tempLabel = new Label(String.format("%.1f°", data.temp));
        tempLabel.setFont(Font.font("System", FontWeight.THIN, 58));
        tempLabel.setTextFill(Color.WHITE);
        tempLabel.setAlignment(Pos.CENTER);

        Label descLabel = new Label(weatherDescription(data.code));
        descLabel.setFont(Font.font("System", 16));
        descLabel.setTextFill(Color.rgb(255, 255, 255, 0.9));
        descLabel.setAlignment(Pos.CENTER);

        Label feelsLabel = new Label(String.format("Percepita %.1f°", data.feelsLike));
        feelsLabel.setFont(Font.font("System", 13));
        feelsLabel.setTextFill(Color.rgb(255, 255, 255, 0.7));
        feelsLabel.setAlignment(Pos.CENTER);

        // Details row
        HBox details = new HBox(30);
        details.setAlignment(Pos.CENTER);

        VBox humBox = detailBox("Umidità", data.humidity + "%");
        VBox windBox = detailBox("Vento", String.format("%.0f km/h", data.wind));

        details.getChildren().addAll(humBox, windBox);

        Line sep = new Line(0, 0, 160, 0);
        sep.setStroke(Color.rgb(255, 255, 255, 0.2));
        sep.setStrokeWidth(1);

        resultCard.getChildren().addAll(cityLabel, tempLabel, descLabel, feelsLabel, sep, details);

        FadeTransition ft = new FadeTransition(Duration.millis(400), resultCard);
        ft.setFromValue(0);
        ft.setToValue(1);
        resultCard.setVisible(true);
        ft.play();
    }

    private VBox detailBox(String label, String value) {
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 18));
        val.setTextFill(Color.WHITE);
        val.setAlignment(Pos.CENTER);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 12));
        lbl.setTextFill(Color.rgb(255, 255, 255, 0.7));
        lbl.setAlignment(Pos.CENTER);
        VBox box = new VBox(2, val, lbl);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private String weatherDescription(int code) {
        if (code == 0) return "\u2600\uFE0F Sereno";
        if (code <= 3) return "\u26C5 Nuvoloso";
        if (code <= 48) return "\uD83C\uDF2B\uFE0F Nebbia";
        if (code <= 57) return "\uD83C\uDF26 Pioggerella";
        if (code <= 67) return "\uD83C\uDF27 Pioggia";
        if (code <= 77) return "\u2744\uFE0F Neve";
        if (code <= 82) return "\uD83C\uDF28 Rovesci";
        if (code <= 86) return "\uD83C\uDF28 Nevischio";
        return "\u26A1 Temporale";
    }

    private record WeatherData(String city, String country, double temp, double feelsLike, int humidity, double wind, int code) {}

    public static void main(String[] args) {
        launch(args);
    }
}
