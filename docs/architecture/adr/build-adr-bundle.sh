#!/usr/bin/env bash
set -euo pipefail

OUT_FILE="ADR_BUNDLE.md"
TMP_FILE="$(mktemp)"

# Cabecera del bundle
{
  echo "# ADR Bundle"
  echo
  echo "> Fichero generado automáticamente. No editar a mano."
  echo "> Fecha de generación: $(date '+%Y-%m-%d %H:%M:%S')"
  echo
  echo "---"
  echo
  echo "## Índice"
  echo
} > "$TMP_FILE"

# Generar índice
find . -maxdepth 1 -type f -name "*.md" \
  ! -name "$OUT_FILE" \
  | sort \
  | while read -r file; do
      base="$(basename "$file")"
      anchor=$(echo "$base" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g')
      echo "- [$base](#file-$anchor)" >> "$TMP_FILE"
    done

{
  echo
  echo "---"
  echo
} >> "$TMP_FILE"

# Concatenar contenido
find . -maxdepth 1 -type f -name "*.md" \
  ! -name "$OUT_FILE" \
  | sort \
  | while read -r file; do
      base="$(basename "$file")"
      anchor=$(echo "$base" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9]/-/g')

      {
        echo "<a name=\"file-$anchor\"></a>"
        echo
        echo "---"
        echo
        echo "# FILE: $base"
        echo
        echo "<!-- BEGIN FILE: $base -->"
        echo
        cat "$file"
        echo
        echo "<!-- END FILE: $base -->"
        echo
      } >> "$TMP_FILE"
    done

mv "$TMP_FILE" "$OUT_FILE"

echo "Generado: $OUT_FILE"