#!/bin/sh
# ---------------------------------------------------------------------------
# Maven Wrapper (versión simplificada): no necesitas Maven instalado.
# La primera vez descarga Maven en ~/.m2/wrapper y luego lo reutiliza.
# Requisito: Java 17 o superior (java -version).
# Uso:  ./mvnw clean test
#       cd ms-catalogo && ../mvnw spring-boot:run
# ---------------------------------------------------------------------------
set -e
BASE_DIR=$(cd "$(dirname "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
URL=$(grep '^distributionUrl=' "$PROPS" | cut -d= -f2-)
NAME=$(basename "$URL" -bin.tar.gz)
DEST="$HOME/.m2/wrapper/dists/$NAME"
MVN="$DEST/$NAME/bin/mvn"

if ! command -v java >/dev/null 2>&1 && [ -z "$JAVA_HOME" ]; then
  echo "ERROR: no se encontró Java. Instala Java 17+ (ej: sdk install java 17.0.12-tem)" >&2
  exit 1
fi

if [ ! -x "$MVN" ]; then
  echo "Descargando $NAME (solo la primera vez)..."
  mkdir -p "$DEST"
  TMP="$DEST/download.tar.gz"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --progress-bar -o "$TMP" "$URL"
  else
    wget -q -O "$TMP" "$URL"
  fi
  tar xzf "$TMP" -C "$DEST"
  rm -f "$TMP"
fi

exec "$MVN" "$@"
