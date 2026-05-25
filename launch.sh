#!/bin/bash
# Avvia Meteo App – richiede Java 17+ e JavaFX SDK

cd "$(dirname "$0")"
# Scarica automaticamente JavaFX se non presente

JAVAFX_DIR="$HOME/.javafx-sdk"
JAVAFX_VERSION="23"

if [ ! -d "$JAVAFX_DIR/lib" ]; then
    echo "Scarico JavaFX SDK $JAVAFX_VERSION..."
    URL="https://download2.gluonhq.com/openjfx/$JAVAFX_VERSION/openjfx-${JAVAFX_VERSION}_linux-x64_bin-sdk.zip"
    curl -L -o /tmp/javafx.zip "$URL"
    mkdir -p "$JAVAFX_DIR"
    unzip -q /tmp/javafx.zip -d "$JAVAFX_DIR"
    mv "$JAVAFX_DIR/javafx-sdk-$JAVAFX_VERSION"/* "$JAVAFX_DIR/"
    rm -rf "$JAVAFX_DIR/javafx-sdk-$JAVAFX_VERSION" /tmp/javafx.zip
fi

# Costruisce il jar se non esiste
JAR="target/weather-app-1.0-SNAPSHOT.jar"
if [ ! -f "$JAR" ]; then
    mvn clean package -q
fi

java --module-path "$JAVAFX_DIR/lib" --add-modules javafx.controls,javafx.fxml -jar "$JAR" 2>"$HOME/.meteoapp-error.log"
