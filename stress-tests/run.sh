#!/bin/bash
# Ejecuta toda la batería de stress tests contra el backend.
# Uso (desde la raíz del repo):  ./stress-tests/run.sh
# Variables opcionales:  BASE_URL, TOKENS (JSON array de JWT), ID_PARTIDO
set -u

echo "Verificando backend..."
curl -sf "${BASE_URL:-http://localhost:8080}/q/health/ready" >/dev/null || {
  echo "El backend no responde en /q/health/ready. Arráncalo primero."
  exit 1
}

mkdir -p stress-tests/resultados

for test in 01-carga-normal 02-pico-apuestas 03-websocket 04-sistema-completo; do
  echo "Ejecutando $test..."
  k6 run --out json=stress-tests/resultados/${test}.json "stress-tests/${test}.js"
  echo "--- $test completado ---"
done

echo "Tests completados. Resultados en stress-tests/resultados/"
