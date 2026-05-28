#!/bin/bash
# Crea un pacchetto .deb per Meteo App (installabile come app di sistema)
# Utilizzo: ./build-deb.sh
# Prerequisiti: JDK 17+, jpackage, dpkg-deb, fakeroot, curl, unzip

set -euo pipefail

VERSION="1.0"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/meteo-app-build"
JAVAFX_HOME="$CACHE_DIR/javafx"
JACKSON_HOME="$CACHE_DIR/jackson"
WORK_DIR=$(mktemp -d)
BUILD_DIR="$WORK_DIR/build"
DIST_DIR="$WORK_DIR/dist"

cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

echo "=== Meteo App — Build .deb ==="

# --- Scarica JavaFX SDK ---
JAVAFX_VERSION=21
if [ ! -f "$JAVAFX_HOME/lib/javafx-controls.jar" ]; then
    echo "Scarico JavaFX SDK $JAVAFX_VERSION..."
    rm -rf "$JAVAFX_HOME"
    mkdir -p "$JAVAFX_HOME"
    URL="https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip"
    curl -#L -o /tmp/javafx.zip "$URL"
    unzip -qo /tmp/javafx.zip -d /tmp/javafx-extract
    mv /tmp/javafx-extract/javafx-sdk-$JAVAFX_VERSION/* "$JAVAFX_HOME/"
    rm -rf /tmp/javafx-extract /tmp/javafx.zip
fi

# Scarica JavaFX jmods (per jlink/jpackage)
JAVAFX_MODS="$CACHE_DIR/javafx-jmods"
if [ ! -f "$JAVAFX_MODS/javafx-controls.jmod" ]; then
    echo "Scarico JavaFX jmods $JAVAFX_VERSION..."
    rm -rf "$JAVAFX_MODS"
    mkdir -p "$JAVAFX_MODS"
    URL="https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-jmods.zip"
    curl -#L -o /tmp/javafx-jmods.zip "$URL"
    unzip -qo /tmp/javafx-jmods.zip -d /tmp/javafx-jmods-extract
    mv /tmp/javafx-jmods-extract/javafx-jmods-$JAVAFX_VERSION/* "$JAVAFX_MODS/"
    rm -rf /tmp/javafx-jmods-extract /tmp/javafx-jmods.zip
fi

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

# --- Compila ---
echo "Compilazione..."
mkdir -p "$BUILD_DIR/com/example/weather"

cat > "$BUILD_DIR/com/example/weather/WeatherApp.java" << 'JAVAEOF'
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
    private Label loadingLabel;
    private TextField cityField;
    private VBox contentArea;
    private WeatherData lastData;
    private String lastCity;
    private double lastLat, lastLon;

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

        contentArea = new VBox(16);
        contentArea.setAlignment(Pos.TOP_CENTER);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        searchBtn.setOnAction(e -> {
            String city = cityField.getText().trim();
            if (city.isEmpty()) return;
            errorLabel.setVisible(false);
            contentArea.getChildren().clear();
            loadingLabel.setVisible(true);
            fetchWeather(city);
        });
        cityField.setOnAction(e -> searchBtn.fire());

        VBox root = new VBox(16, title, searchRow, errorLabel, loadingLabel, contentArea);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(30, 20, 30, 20));
        root.setBackground(new Background(new BackgroundFill(
            new LinearGradient(0, 0, 0, 1, true, null,
                new Stop(0, Color.web("#0f2027")),
                new Stop(0.5, Color.web("#203a43")),
                new Stop(1, Color.web("#2c5364"))),
            CornerRadii.EMPTY, Insets.EMPTY)));

        primaryStage.setScene(new Scene(root, 500, 580));
        primaryStage.setTitle("Meteo App");
        primaryStage.show();
    }

    // --- Fetch ---

    private void fetchWeather(String city) {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded
                + "&count=1&language=it&format=json";

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                JsonNode first = geocode(geoUrl, city);
                lastCity = first.get("name").asText();
                String country = first.has("country_code") ? first.get("country_code").asText().toUpperCase() : "";
                lastLat = first.get("latitude").asDouble();
                lastLon = first.get("longitude").asDouble();

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                    "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m",
                    lastLat, lastLon);
                JsonNode w = fetchJson(weatherUrl).at("/current");

                lastData = new WeatherData(lastCity, country,
                    w.get("temperature_2m").asDouble(),
                    w.get("apparent_temperature").asDouble(),
                    w.get("relative_humidity_2m").asInt(),
                    w.get("wind_speed_10m").asDouble(),
                    w.get("weather_code").asInt());
                javafx.application.Platform.runLater(() -> showChoiceButtons());
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

    // --- Views ---

    private void showChoiceButtons() {
        loadingLabel.setVisible(false);
        contentArea.getChildren().clear();

        Label cityLabel = new Label(lastData.city + (lastData.country().isEmpty() ? "" : ", " + lastData.country()));
        cityLabel.setFont(Font.font("System", FontWeight.SEMI_BOLD, 15));
        cityLabel.setTextFill(Color.rgb(255, 255, 255, 0.85));

        Button currentBtn = styledButton("Condizioni attuali");
        currentBtn.setOnMouseEntered(e -> styleHover(currentBtn, "#059669", "rgba(5,150,105,0.5)"));
        currentBtn.setOnMouseExited(e -> styleHover(currentBtn, "#059669", "rgba(5,150,105,0.4)"));
        currentBtn.setOnAction(e -> showCurrent());

        Button reportBtn = styledButton("Report giornaliero");
        reportBtn.setOnMouseEntered(e -> styleHover(reportBtn, "#d97706", "rgba(217,119,6,0.5)"));
        reportBtn.setOnMouseExited(e -> styleHover(reportBtn, "#d97706", "rgba(217,119,6,0.4)"));
        reportBtn.setOnAction(e -> showReport());

        HBox btnRow = new HBox(14, currentBtn, reportBtn);
        btnRow.setAlignment(Pos.CENTER);

        contentArea.getChildren().addAll(cityLabel, btnRow);
    }

    private void showCurrent() {
        contentArea.getChildren().clear();
        showChoiceButtons();

        VBox card = createResultCard();
        Label tempLabel = new Label(String.format("%.1f°", lastData.temp()));
        tempLabel.setFont(Font.font("System", FontWeight.THIN, 58));
        tempLabel.setTextFill(Color.WHITE);
        tempLabel.setAlignment(Pos.CENTER);

        Label descLabel = new Label(weatherDescription(lastData.code()));
        descLabel.setFont(Font.font("System", 16));
        descLabel.setTextFill(Color.rgb(255, 255, 255, 0.9));
        descLabel.setAlignment(Pos.CENTER);

        Label feelsLabel = new Label(String.format("Percepita %.1f°", lastData.feelsLike()));
        feelsLabel.setFont(Font.font("System", 13));
        feelsLabel.setTextFill(Color.rgb(255, 255, 255, 0.7));
        feelsLabel.setAlignment(Pos.CENTER);

        HBox details = new HBox(30);
        details.setAlignment(Pos.CENTER);
        details.getChildren().addAll(
            detailBox("Umidità", lastData.humidity() + "%"),
            detailBox("Vento", String.format("%.0f km/h", lastData.wind())));

        Line sep = new Line(0, 0, 160, 0);
        sep.setStroke(Color.rgb(255, 255, 255, 0.2));
        sep.setStrokeWidth(1);

        card.getChildren().addAll(tempLabel, descLabel, feelsLabel, sep, details);

        StackPane cardContainer = new StackPane(card);
        cardContainer.setAlignment(Pos.CENTER);
        cardContainer.setMaxWidth(400);

        FadeTransition ft = new FadeTransition(Duration.millis(400), card);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();

        contentArea.getChildren().add(cardContainer);
    }

    private void showReport() {
        contentArea.getChildren().clear();
        showChoiceButtons();

        Spinner<Integer> daySpinner = new Spinner<>(1, 16, 7);
        daySpinner.setEditable(true);
        daySpinner.setPrefWidth(80);
        daySpinner.setStyle("-fx-font-size: 14px; -fx-background-radius: 10;");

        Label dayLabel = new Label("Giorni:");
        dayLabel.setTextFill(Color.rgb(255, 255, 255, 0.8));

        Button generateBtn = styledButton("Genera");
        generateBtn.setOnMouseEntered(e -> styleHover(generateBtn, "#2563eb", "rgba(37,99,235,0.5)"));
        generateBtn.setOnMouseExited(e -> styleHover(generateBtn, "#3b82f6", "rgba(59,130,246,0.4)"));

        HBox controls = new HBox(10, dayLabel, daySpinner, generateBtn);
        controls.setAlignment(Pos.CENTER);

        VBox reportContent = new VBox(12);
        reportContent.setAlignment(Pos.CENTER);

        contentArea.getChildren().add(controls);

        generateBtn.setOnAction(e -> {
            reportContent.getChildren().clear();
            loadingLabel.setVisible(true);
            String weatherUrl = String.format(
                "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f" +
                "&daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max" +
                "&timezone=auto&forecast_days=%d", lastLat, lastLon, daySpinner.getValue());

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
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
                    javafx.application.Platform.runLater(() -> {
                        loadingLabel.setVisible(false);
                        showChart(reportContent, list, daySpinner.getValue());
                    });
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
        });

        contentArea.getChildren().add(reportContent);
    }

    private void showChart(VBox container, List<DailyData> data, int days) {
        container.getChildren().clear();

        double avgTMax = data.stream().mapToDouble(DailyData::tempMax).average().orElse(0);
        double avgTMin = data.stream().mapToDouble(DailyData::tempMin).average().orElse(0);
        double avgWind = data.stream().mapToDouble(DailyData::windMax).average().orElse(0);

        HBox avgs = new HBox(20);
        avgs.setAlignment(Pos.CENTER);
        avgs.getChildren().addAll(
            avgBox("T Max media", String.format("%.1f°C", avgTMax), "#ff6b6b"),
            avgBox("T Min media", String.format("%.1f°C", avgTMin), "#4ecdc4"),
            avgBox("Vento medio", String.format("%.0f km/h", avgWind), "#ffe66d"));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temperatura (°C)");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Andamento " + days + " giorni");
        chart.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-tick-label-fill: white;");
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setPrefHeight(220);
        chart.setCreateSymbols(true);
        chart.setMaxWidth(440);
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

        container.getChildren().addAll(avgs, chart);
    }

    private VBox avgBox(String label, String value, String color) {
        Label val = new Label(value);
        val.setFont(Font.font("System", FontWeight.BOLD, 16));
        val.setTextFill(Color.web(color));
        val.setAlignment(Pos.CENTER);
        Label lbl = new Label(label);
        lbl.setFont(Font.font("System", 11));
        lbl.setTextFill(Color.rgb(255, 255, 255, 0.7));
        lbl.setAlignment(Pos.CENTER);
        VBox box = new VBox(2, val, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 14, 10, 14));
        box.setBackground(new Background(new BackgroundFill(
            Color.rgb(255, 255, 255, 0.06), new CornerRadii(12), Insets.EMPTY)));
        return box;
    }

    // --- Shared helpers ---

    private JsonNode geocode(String geoUrl, String city) throws Exception {
        JsonNode geoData = fetchJson(geoUrl);
        JsonNode results = geoData.get("results");
        if (results == null || !results.isArray() || results.isEmpty())
            throw new RuntimeException("Località \"" + city + "\" non trovata.");
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
            "-fx-background-color: #3b82f6; -fx-background-radius: 22; -fx-padding: 10 22; " +
            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.4), 8, 0, 0, 4);");
        return btn;
    }

    private void styleHover(Button btn, String color, String shadow) {
        btn.setStyle(
            "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: white; " +
            "-fx-background-color: " + color + "; -fx-background-radius: 22; -fx-padding: 10 22; " +
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

mkdir -p "$BUILD_DIR/classes"
javac --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls \
    -d "$BUILD_DIR/classes" -cp "$JACKSON_LIBS" \
    "$BUILD_DIR/com/example/weather/WeatherApp.java"

# --- Crea fat JAR con Jackson incluso ---
echo "Creazione JAR..."
mkdir -p "$DIST_DIR"
cd "$BUILD_DIR/classes"

# Estrai Jackson nel classpath per includerlo nel JAR
for jar in $(echo "$JACKSON_LIBS" | tr ':' ' '); do
    unzip -qo "$jar" -d "$BUILD_DIR/classes/"
done

jar cfe "$DIST_DIR/meteoapp.jar" com.example.weather.WeatherApp .
cd "$OLDPWD"

# --- Crea icona ---
mkdir -p "$BUILD_DIR/icon"
# Genera un'icona PNG semplice (cerchio blu con bianco)
# Se ImageMagick è disponibile, crea un'icona decorosa
if command -v convert &>/dev/null; then
    convert -size 256x256 xc:'#203a43' -fill white \
        -font Helvetica -pointsize 100 -gravity center \
        -annotate 0 '°C' -fill '#3b82f6' \
        -draw "circle 128,128 128,20" -fill none -stroke white -strokewidth 4 \
        -draw "circle 128,128 128,20" \
        "$BUILD_DIR/icon/meteoapp.png" 2>/dev/null || \
    convert -size 256x256 xc:'#3b82f6' -fill white \
        -font Helvetica -pointsize 100 -gravity center \
        -annotate 0 '°C' \
        "$BUILD_DIR/icon/meteoapp.png" 2>/dev/null || \
        # Fallback: copia logo esistente
        cp "$SCRIPT_DIR/logoMeteo.png" "$BUILD_DIR/icon/meteoapp.png" 2>/dev/null || true
else
    cp "$SCRIPT_DIR/logoMeteo.png" "$BUILD_DIR/icon/meteoapp.png" 2>/dev/null || true
fi

echo "Creazione pacchetto .deb con jpackage..."

# Se non c'è icona, jpackage si lamenta, quindi gestiamo
ICON_ARG=""
[ -f "$BUILD_DIR/icon/meteoapp.png" ] && ICON_ARG="--icon $BUILD_DIR/icon/meteoapp.png"

jpackage \
    --type deb \
    --name "MeteoApp" \
    --app-version "$VERSION" \
    --description "Visualizza il meteo di una località" \
    --vendor "PCTO" \
    --main-class com.example.weather.WeatherApp \
    --main-jar meteoapp.jar \
    --input "$DIST_DIR" \
    --module-path "$JAVAFX_MODS" \
    --add-modules javafx.controls,java.net.http,jdk.crypto.ec \
    --dest "$WORK_DIR/installer" \
    --verbose \
    $ICON_ARG

# --- Copia risultato ---
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
INSTALLER=$(ls "$WORK_DIR/installer/"*.deb 2>/dev/null || true)
if [ -n "$INSTALLER" ]; then
    cp "$INSTALLER" "$SCRIPT_DIR/"
    echo ""
    echo "=== FATTO ==="
    echo "Pacchetto creato: $(basename "$INSTALLER")"
    echo "Installalo con: sudo dpkg -i $(basename "$INSTALLER")"
    echo "Poi avvia dal menu applicazioni: MeteoApp"
else
    echo "ERRORE: jpackage non ha prodotto un .deb. Verifica i log sopra." >&2
    exit 1
fi
