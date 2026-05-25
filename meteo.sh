#!/bin/bash
# Meteo App — script autosufficiente e portabile
# Utilizzo: ./meteo.sh [OPZIONE]
#   --install    Installa in ~/.local/bin/meteo (aggiungi al PATH)
#   --help       Mostra questo messaggio
#
# Richiede: Java 17+ (JDK con javac), curl, unzip
# Supporta: Linux (x86_64, aarch64), macOS (x86_64, arm64)

set -euo pipefail

VERSION="1.0"

usage() {
    sed -n '3,/^$/ s/^# //p' "$0"
    exit 0
}

# --- Rilevamento piattaforma ---
OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
ARCH="$(uname -m)"

case "$OS" in
    linux)  JFX_OS="linux" ;;
    darwin) JFX_OS="osx"   ;;
    *)
        echo "Errore: sistema operativo non supportato ($OS)." >&2
        exit 1
        ;;
esac

case "$ARCH" in
    x86_64|amd64)  JFX_ARCH="x64"       ;;
    aarch64|arm64) JFX_ARCH="aarch64"    ;;
    *)
        echo "Errore: architettura non supportata ($ARCH)." >&2
        exit 1
        ;;
esac

# --- Directory ---
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
case "$OS" in
    linux)   CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/meteo-app" ;;
    darwin)  CACHE_DIR="$HOME/Library/Caches/meteo-app"            ;;
esac
JAVAFX_HOME="$CACHE_DIR/javafx"
JACKSON_HOME="$CACHE_DIR/jackson"

# --- Installa in PATH ---
if [ "${1:-}" = "--install" ]; then
    INSTALL_DIR="${HOME}/.local/bin"
    mkdir -p "$INSTALL_DIR"
    cp "$0" "$INSTALL_DIR/meteo"
    chmod +x "$INSTALL_DIR/meteo"
    echo "Installato in $INSTALL_DIR/meteo"
    echo "Assicurati che $INSTALL_DIR sia nel tuo PATH."
    echo "Poi usa: meteo"
    exit 0
fi

[ "${1:-}" = "--help" ] && usage

WORK_DIR=$(mktemp -d)
cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

# --- Verifica Java ---
if ! command -v java &>/dev/null || ! command -v javac &>/dev/null; then
    echo "Errore: servono java e javac (JDK 17+)." >&2
    exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/[^0-9]*//; s/\..*//')
if [ "$JAVA_VER" -lt 17 ]; then
    echo "Errore: serve Java 17+ (trovato: $(java -version 2>&1 | head -1))." >&2
    exit 1
fi

# --- Scarica JavaFX SDK ---
JAVAFX_VERSION=23
JFX_FILE="openjfx-${JAVAFX_VERSION}_${JFX_OS}-${JFX_ARCH}_bin-sdk.zip"

if [ ! -f "$JAVAFX_HOME/lib/javafx-controls.jar" ]; then
    echo "Scarico JavaFX SDK $JAVAFX_VERSION ($JFX_OS-$JFX_ARCH)..."
    mkdir -p "$JAVAFX_HOME"
    URL="https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/$JFX_FILE"
    curl -#L -o /tmp/javafx.zip "$URL"
    unzip -qo /tmp/javafx.zip -d "$JAVAFX_HOME"
    mv "$JAVAFX_HOME/javafx-sdk-$JAVAFX_VERSION"/* "$JAVAFX_HOME/"
    rm -rf "$JAVAFX_HOME/javafx-sdk-$JAVAFX_VERSION" /tmp/javafx.zip
fi

JAVAFX_LIBS=$(echo "$JAVAFX_HOME/lib"/javafx*.jar | tr ' ' ':')

# --- Scarica Jackson ---
JACKSON_VERSION=2.17.1
if [ ! -f "$JACKSON_HOME/jackson-databind.jar" ]; then
    echo "Scarico Jackson $JACKSON_VERSION..."
    mkdir -p "$JACKSON_HOME"
    for ARTIFACT in jackson-core jackson-databind jackson-annotations; do
        URL="https://repo1.maven.org/maven2/com/fasterxml/jackson/core/$ARTIFACT/$JACKSON_VERSION/$ARTIFACT-$JACKSON_VERSION.jar"
        curl -#L -o "$JACKSON_HOME/$ARTIFACT.jar" "$URL"
    done
fi

JACKSON_LIBS=$(echo "$JACKSON_HOME"/*.jar | tr ' ' ':')

# --- Estrai e compila il sorgente ---
mkdir -p "$WORK_DIR/com/example/weather"

cat > "$WORK_DIR/com/example/weather/WeatherApp.java" << 'JAVAEOF'
package com.example.weather;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherApp extends Application {
    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private Label errorLabel;
    private VBox resultCard;
    private Label loadingLabel;
    private TextField cityField;
    private TabPane tabPane;
    private VBox reportCard;
    private Label reportLoading;
    private Label reportError;
    private Spinner<Integer> daySpinner;

    private record WeatherData(String city, String country, double temp, double feelsLike, int humidity, double wind, int code) {}
    private record DailyData(String date, double tempMax, double tempMin, double windMax) {}

    @Override
    public void start(Stage primaryStage) {
        Label title = new Label("Meteo App");
        title.setFont(Font.font("System", FontWeight.BOLD, 26));
        title.setTextFill(Color.WHITE);

        cityField = new TextField();
        cityField.setPromptText("Roma, Milano, Tokyo...");
        cityField.setStyle(
            "-fx-font-size: 14px; -fx-padding: 10 14; -fx-background-radius: 22; " +
            "-fx-border-radius: 22; -fx-background-color: white; -fx-prompt-text-fill: #999;");
        cityField.setMaxWidth(300);

        Button searchBtn = styledButton("CERCA");
        searchBtn.setOnMouseEntered(e -> styleHover(searchBtn, "#2563eb", "rgba(37,99,235,0.5)"));
        searchBtn.setOnMouseExited(e -> styleHover(searchBtn, "#3b82f6", "rgba(59,130,246,0.4)"));

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
        StackPane cardContainer = new StackPane(resultCard);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setMaxWidth(400);

        Tab currentTab = new Tab("Corrente");
        currentTab.setClosable(false);
        currentTab.setContent(new StackPane(cardContainer));

        reportCard = new VBox(12);
        reportCard.setAlignment(Pos.CENTER);
        reportCard.setVisible(false);
        StackPane reportContainer = new StackPane(reportCard);
        reportContainer.setAlignment(Pos.CENTER);

        Tab reportTab = new Tab("Report");
        reportTab.setClosable(false);
        reportTab.setContent(reportContainer);

        tabPane = new TabPane(currentTab, reportTab);
        tabPane.setTabMinWidth(120);
        tabPane.setStyle(
            "-fx-background-color: transparent; -fx-tab-border-color: transparent; " +
            " -fx-tab-label-background: transparent;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        reportLoading = new Label("Caricamento...");
        reportLoading.setFont(Font.font("System", 16));
        reportLoading.setTextFill(Color.rgb(255, 255, 255, 0.8));
        reportLoading.setVisible(false);

        reportError = new Label();
        reportError.setFont(Font.font("System", 13));
        reportError.setTextFill(Color.rgb(255, 200, 200, 0.95));
        reportError.setWrapText(true);
        reportError.setAlignment(Pos.CENTER);
        reportError.setMaxWidth(380);
        reportError.setVisible(false);

        daySpinner = new Spinner<>(1, 16, 7);
        daySpinner.setEditable(true);
        daySpinner.setPrefWidth(80);
        daySpinner.setStyle("-fx-font-size: 14px; -fx-background-radius: 10;");

        searchBtn.setOnAction(e -> {
            errorLabel.setVisible(false);
            resultCard.setVisible(false);
            if (tabPane.getSelectionModel().getSelectedIndex() == 0) {
                searchCurrent();
            } else {
                searchReport();
            }
        });
        cityField.setOnAction(e -> searchBtn.fire());

        VBox root = new VBox(16, title, searchRow, errorLabel, loadingLabel, tabPane);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30, 20, 30, 20));
        root.setBackground(new Background(new BackgroundFill(
            new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#0f2027")),
                new Stop(0.5, Color.web("#203a43")),
                new Stop(1, Color.web("#2c5364"))),
            CornerRadii.EMPTY, Insets.EMPTY)));

        primaryStage.setScene(new Scene(root, 500, 560));
        primaryStage.setTitle("Meteo App");
        primaryStage.show();
    }

    private void searchCurrent() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;
        loadingLabel.setVisible(true);
        fetchWeather(city);
    }

    private void searchReport() {
        String city = cityField.getText().trim();
        if (city.isEmpty()) return;
        loadingLabel.setVisible(true);
        reportCard.setVisible(false);
        reportError.setVisible(false);
        fetchReport(city, daySpinner.getValue());
    }

    private void fetchWeather(String city) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonNode first = geocode(geoUrl, city);
                String cityName = first.get("name").asText();
                String country = first.has("country_code") ? first.get("country_code").asText().toUpperCase() : "";
                double lat = first.get("latitude").asDouble();
                double lon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    lat, lon);
                JsonNode w = fetchJson(weatherUrl).at("/current");

                WeatherData data = new WeatherData(cityName, country,
                    w.get("temperature_2m").asDouble(),
                    w.get("apparent_temperature").asDouble(),
                    w.get("relative_humidity_2m").asInt(),
                    w.get("wind_speed_10m").asDouble(),
                    w.get("weather_code").asInt());
                javafx.application.Platform.runLater(() -> showResult(data));
                return null;
            }

            @Override
            protected void failed() {
                loadingLabel.setVisible(false);
                errorLabel.setText(getException().getMessage());
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

        HBox details = new HBox(30);
        details.setAlignment(Pos.CENTER);
        details.getChildren().addAll(
            detailBox("Umidità", data.humidity + "%"),
            detailBox("Vento", String.format("%.0f km/h", data.wind)));

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

    private void fetchReport(String city, int days) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonNode first = geocode(geoUrl, city);
                double lat = first.get("latitude").asDouble();
                double lon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max" +
                    "&timezone=auto&forecast_days=%d", lat, lon, days);
                JsonNode daily = fetchJson(weatherUrl).get("daily");

                List<DailyData> list = new ArrayList<>();
                JsonNode dates = daily.get("time");
                JsonNode tMax = daily.get("temperature_2m_max");
                JsonNode tMin = daily.get("temperature_2m_min");
                JsonNode wMax = daily.get("wind_speed_10m_max");
                for (int i = 0; i < dates.size(); i++) {
                    list.add(new DailyData(
                        dates.get(i).asText(),
                        tMax.get(i).asDouble(),
                        tMin.get(i).asDouble(),
                        wMax.get(i).asDouble()));
                }
                javafx.application.Platform.runLater(() -> showReport(list));
                return null;
            }

            @Override
            protected void failed() {
                loadingLabel.setVisible(false);
                reportError.setText(getException().getMessage());
                reportError.setVisible(true);
            }
        };
        new Thread(task).start();
    }

    private void showReport(List<DailyData> data) {
        loadingLabel.setVisible(false);
        reportCard.getChildren().clear();

        Label title = new Label("Report " + daySpinner.getValue() + " giorni");
        title.setFont(Font.font("System", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);

        HBox controls = new HBox(10, new Label("Giorni:"), daySpinner);
        controls.setAlignment(Pos.CENTER);
        Label dayLabel = new Label("Giorni:");
        dayLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));
        controls.getChildren().set(0, dayLabel);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temperatura (°C)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Andamento " + daySpinner.getValue() + " giorni");
        chart.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-tick-label-fill: white;");
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(260);
        chart.setCreateSymbols(true);
        chart.lookup(".chart-legend").setStyle("-fx-background-color: transparent; -fx-text-fill: white;");

        xAxis.setTickLabelRotation(45);

        XYChart.Series<String, Number> maxSeries = new XYChart.Series<>();
        maxSeries.setName("T Max");
        XYChart.Series<String, Number> minSeries = new XYChart.Series<>();
        minSeries.setName("T Min");
        XYChart.Series<String, Number> windSeries = new XYChart.Series<>();
        windSeries.setName("Vento (km/h)");

        for (DailyData d : data) {
            String label = d.date.substring(5);
            maxSeries.getData().add(new XYChart.Data<>(label, d.tempMax));
            minSeries.getData().add(new XYChart.Data<>(label, d.tempMin));
            windSeries.getData().add(new XYChart.Data<>(label, d.windMax));
        }

        chart.getData().addAll(maxSeries, minSeries, windSeries);

        maxSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #ff6b6b;");
        minSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #4ecdc4;");
        windSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #ffe66d;");

        reportCard.getChildren().addAll(title, controls, chart);
        reportCard.setVisible(true);
    }

    private JsonNode geocode(String geoUrl, String city) throws Exception {
        JsonNode geoData = fetchJson(geoUrl);
        JsonNode results = geoData.get("results");
        if (results == null || !results.isArray() || results.isEmpty())
            throw new RuntimeException("Localita \"" + city + "\" non trovata.");
        return results.get(0);
    }

    private JsonNode fetchJson(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("Errore API: " + resp.statusCode() + " " + resp.body());
        return mapper.readTree(resp.body());
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: #3b82f6; -fx-background-radius: 22; -fx-padding: 10 28; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 4);");
        return btn;
    }

    private void styleHover(Button btn, String color, String shadow) {
        btn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: " + color + "; -fx-background-radius: 22; -fx-padding: 10 28; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, " + shadow + ", 12, 0, 0, 6);");
    }

    private VBox createResultCard() {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(24, 28, 24, 28));
        card.setBackground(new Background(new BackgroundFill(
            Color.rgb(255, 255, 255, 0.12), new CornerRadii(20), Insets.EMPTY)));
        card.setMaxWidth(400);
        card.setEffect(new DropShadow(20, Color.rgb(0, 0, 0, 0.3)));
        return card;
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

    public static void main(String[] args) {
        launch(args);
    }
}
JAVAEOF

echo "Compilazione in corso..."
javac -d "$WORK_DIR/classes" -cp "$JAVAFX_LIBS:$JACKSON_LIBS" \
    "$WORK_DIR/com/example/weather/WeatherApp.java"

echo "Avvio Meteo App..."
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls \
    -cp "$WORK_DIR/classes:$JACKSON_LIBS" \
    com.example.weather.WeatherApp
