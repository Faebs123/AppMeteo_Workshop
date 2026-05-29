#!/bin/bash
# Meteo App — script autosufficient and portable
# Usage: ./meteo.sh [OPTION]
#   --install    Installs in ~/.local/bin/meteo
#   --help       Shows this help
#
# Requires: Java 17+, curl, unzip
# Supports: Linux and macOS

set -euo pipefail

VERSION="1.0"

usage() {
    sed -n '3,/^$/ s/^# //p' "$0"
    exit 0
}

# Detect OS & setup cache dir
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)
case "$OS" in
    linux)   CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/meteo-app" ;;
    darwin)  CACHE_DIR="$HOME/Library/Caches/meteo-app" ;;
esac
JACKSON_HOME="$CACHE_DIR/jackson"

# Install shortcut
if [ "${1:-}" = "--install" ]; then
    INSTALL_DIR="$HOME/.local/bin"
    mkdir -p "$INSTALL_DIR"
    cp "$0" "$INSTALL_DIR/meteo"
    chmod +x "$INSTALL_DIR/meteo"
    echo "Installed in $INSTALL_DIR/meteo"
    echo "Add $INSTALL_DIR to your PATH and run 'meteo'"
    exit 0
fi
[ "${1:-}" = "--help" ] && usage

WORK_DIR=$(mktemp -d)
trap 'rm -rf "$WORK_DIR"' EXIT

# Check Java
command -v java &>/dev/null || { echo 'Error: Java required'; exit 1; }
command -v javac &>/dev/null || { echo 'Error: javac required'; exit 1; }
    JAVA_VER=$(java -version 2>&1 | head -1 | sed 's/[^0-9]*//; s/\..*//')
[ "$JAVA_VER" -lt 17 ] && { echo "Error: Java 17+ required"; exit 1; }

# Download Jackson
JACKSON_VERSION=2.17.1
if [ ! -f "$JACKSON_HOME/jackson-databind.jar" ]; then
    echo "Downloading Jackson $JACKSON_VERSION..."
    mkdir -p "$JACKSON_HOME"
    for ART in jackson-core jackson-databind jackson-annotations; do
        curl -#L -o "$JACKSON_HOME/${ART}.jar" "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/${ART}/${JACKSON_VERSION}/${ART}-${JACKSON_VERSION}.jar"
    done
fi
JACKSON_LIBS=$(printf '%s:' "$JACKSON_HOME"/*.jar | sed 's/:$//')

# Prepare temp source tree
BASE="$WORK_DIR/com/example/weather"
find src/main/java/com/example/weather -name '*.java' | while read -r f; do
    rel="${f#src/main/java/com/example/weather/}"
    mkdir -p "$BASE/$(dirname "$rel")"
    cp "$f" "$BASE/$rel"
done

# Compile
echo 'Compiling Java sources...'
javac -d "$WORK_DIR/classes" -cp "$JACKSON_LIBS" $(find "$BASE" -name '*.java')

# Run the app
echo 'Launching Meteo App...'
java -cp "$WORK_DIR/classes:$JACKSON_LIBS" com.example.weather.WeatherApp
