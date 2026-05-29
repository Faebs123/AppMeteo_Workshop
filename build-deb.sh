#!/bin/bash
# Crea un pacchetto .deb per Meteo App (installabile come app di sistema)
# Utilizzo: ./build-deb.sh
# Prerequisiti: JDK 17+, jpackage, dpkg-deb, fakeroot, curl, unzip

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
VERSION="1.0"
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/meteo-app-build"
JACKSON_HOME="$CACHE_DIR/jackson"
FLATLAF_HOME="$CACHE_DIR/flatlaf"
WORK_DIR=$(mktemp -d)
BUILD_DIR="$WORK_DIR/build"
DIST_DIR="$WORK_DIR/dist"

cleanup() { rm -rf "$WORK_DIR"; }
trap cleanup EXIT

echo "=== Meteo App — Build .deb ==="

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

# --- Scarica FlatLaf ---
FLATLAF_VERSION=3.7.1
if [ ! -f "$FLATLAF_HOME/flatlaf.jar" ]; then
    echo "Scarico FlatLaf $FLATLAF_VERSION..."
    mkdir -p "$FLATLAF_HOME"
    curl -#L -o "$FLATLAF_HOME/flatlaf.jar" \
        "https://repo1.maven.org/maven2/com/formdev/flatlaf/${FLATLAF_VERSION}/flatlaf-${FLATLAF_VERSION}.jar"
fi
FLATLAF_LIBS="$FLATLAF_HOME/flatlaf.jar"
ALL_LIBS="${JACKSON_LIBS}:${FLATLAF_LIBS}"

# --- Compila ---
echo "Compilazione..."
mkdir -p "$BUILD_DIR/classes"
BASE="$BUILD_DIR/com/example/weather"
find "$SCRIPT_DIR/src/main/java/com/example/weather" -name '*.java' | while read -r f; do
    rel="${f#$SCRIPT_DIR/src/main/java/com/example/weather/}"
    mkdir -p "$BASE/$(dirname "$rel")"
    cp "$f" "$BASE/$rel"
done
javac -d "$BUILD_DIR/classes" -cp "$ALL_LIBS" $(find "$BASE" -name '*.java')

# --- Crea fat JAR con Jackson e FlatLaf inclusi ---
echo "Creazione JAR..."
mkdir -p "$DIST_DIR"
cd "$BUILD_DIR/classes"

for jar in $(echo "$ALL_LIBS" | tr ':' ' '); do
    unzip -qo "$jar" -d "$BUILD_DIR/classes/"
done

jar cfe "$DIST_DIR/meteoapp.jar" com.example.weather.WeatherApp .
cd "$OLDPWD"

# --- Icona ---
mkdir -p "$BUILD_DIR/icon"
cp "$SCRIPT_DIR/logoMeteo.png" "$BUILD_DIR/icon/meteoapp.png" 2>/dev/null || true

echo "Creazione pacchetto .deb con jpackage..."

ICON_ARG=""
[ -f "$BUILD_DIR/icon/meteoapp.png" ] && ICON_ARG="--icon $BUILD_DIR/icon/meteoapp.png"

jpackage \
    --type deb \
    --name "MeteoApp" \
    --app-version "$VERSION" \
    --description "Visualizza il meteo di una localit\u00e0" \
    --vendor "PCTO" \
    --main-class com.example.weather.WeatherApp \
    --main-jar meteoapp.jar \
    --input "$DIST_DIR" \
    --add-modules java.net.http,jdk.crypto.ec,java.desktop \
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
