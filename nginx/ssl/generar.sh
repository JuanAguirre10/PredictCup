#!/bin/bash
# Genera un certificado TLS autofirmado para desarrollo (no usar en produccion real).
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# MSYS_NO_PATHCONV evita que Git-Bash/MSYS en Windows convierta el "/C=PE/..."
# en una ruta de disco. Con nombres de archivo relativos no hace falta conversion.
# (En Linux/macOS la variable simplemente se ignora.)
MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout key.pem -out cert.pem \
  -subj "/C=PE/ST=Lima/O=TECSUP/CN=localhost"

echo "Certificado autofirmado generado en $DIR (cert.pem, key.pem)."
