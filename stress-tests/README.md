# Stress tests (k6) — Mundial Betting

Pruebas de carga del backend con [k6](https://k6.io). Cuatro escenarios que van de la
operación normal al pico de apuestas, más un test de WebSockets.

## Instalar k6

| Sistema | Comando |
|---|---|
| Windows | `winget install k6 --source winget` · o `choco install k6` |
| macOS | `brew install k6` |
| Linux (Debian/Ubuntu) | `sudo gpg -k && sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69 && echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \| sudo tee /etc/apt/sources.list.d/k6.list && sudo apt update && sudo apt install k6` |
| Docker | `docker run --rm -i grafana/k6 run - < stress-tests/01-carga-normal.js` |

Verifica: `k6 version`.

## Preparar el entorno

1. Levanta el backend (Postgres + Redis). Con Dev Services basta:
   `cd PredictCup && ./mvnw quarkus:dev`
2. Comprueba salud: `curl http://localhost:8080/q/health/ready` → `"status":"UP"`.
3. Para las pruebas de **POST /api/apuestas** necesitas:
   - **TOKENS**: un array JSON de JWT válidos (los emite `/auth/callback` tras el login de
     Google). Sin tokens válidos el POST responde `401` y el escenario lo reflejará.
   - **ID_PARTIDO**: el UUID de un partido con apuestas abiertas
     (`GET /api/partidos?fecha=YYYY-MM-DD` → copia un `id` con `apuestasAbiertas:true`).

## Ejecutar

Todos, con resultados en JSON:

```bash
./stress-tests/run.sh
# o, pasando configuración:
BASE_URL=http://localhost:8080 \
TOKENS='["eyJ...token1","eyJ...token2"]' \
ID_PARTIDO=2b1f...uuid \
./stress-tests/run.sh
```

Uno individual:

```bash
# Carga normal (500 VUs, 2 min)
k6 run stress-tests/01-carga-normal.js

# Pico de apuestas (hasta 1000 VUs)
k6 run --env ID_PARTIDO=<uuid> --env TOKENS='["<jwt>"]' stress-tests/02-pico-apuestas.js

# WebSockets (300 conexiones)
k6 run stress-tests/03-websocket.js

# Sistema completo (informe final, hasta 1000 VUs)
k6 run stress-tests/04-sistema-completo.js

# Smoke rápido (sobrescribe VUs/duración desde la CLI)
k6 run --vus 50 --duration 30s stress-tests/01-carga-normal.js
```

## Qué significa cada métrica

| Métrica | Qué mide |
|---|---|
| `http_req_duration` | Latencia de las peticiones. Miramos el **p(95)**: el 95% va por debajo de ese valor. Es lo que percibe casi todo el mundo (ignora la cola lenta). |
| `http_req_failed` | Proporción de peticiones fallidas a nivel de transporte/HTTP (timeouts, cortes, 5xx). |
| `errores` (custom) | Tasa de **status inesperado** según la lógica de negocio. Clave: un `429` **no** se cuenta como error (es la respuesta correcta del rate limiter). |
| `checks` | % de aserciones que pasan. Se puede filtrar por nombre, p.ej. `checks{check:no hay 500}`. |
| `ws_connecting` | Tiempo del handshake del WebSocket. p(95) bajo = el servidor acepta conexiones rápido aun con muchas abiertas. |
| `vus` / `vus_max` | Usuarios virtuales activos / máximo. |
| `iterations` | Vueltas completas a la función `default` (≈ peticiones de negocio). |

### Thresholds por escenario

- **01 carga-normal**: `p(95)<200ms`, `errores<2%`.
- **02 pico-apuestas**: `checks{check:no hay 500}==1.0` (ni un solo 500), `p(95)<2000ms`.
- **03 websocket**: `ws_connecting p(95)<1000ms`.
- **04 sistema-completo**: `p(95)<500ms` global, lecturas `p(95)<400ms`, `errores<5%`.

## Por qué `429` y `503` son respuestas CORRECTAS bajo sobrecarga

Bajo presión extrema, **degradar de forma controlada es éxito, no fallo**:

- **`429 Too Many Requests`** — lo devuelve el `RateLimitFilter` (Redis INCR, 30 req/min por
  usuario). Significa "te estoy protegiendo a ti y al sistema". El cliente reintenta con
  *backoff*. Por eso los escenarios cuentan `202 || 429` como **OK**.
- **`503 Service Unavailable`** — lo puede devolver Nginx/el balanceador cuando una instancia
  está saturada o reiniciando; el cliente reintenta en otra. Es la red protegiéndose.
- **El fallo de verdad es `500`** — un error no controlado (excepción, pool agotado sin
  manejo, NPE). Por eso `02-pico-apuestas` exige **0 quinientos** como invariante dura.

La arquitectura está pensada para esto: las apuestas **no se escriben de forma síncrona**;
se encolan en Redis (`202` inmediato) y un worker las drena al ritmo del pool de la BD. Así
el pico se absorbe en la cola, no en PostgreSQL.

## Cómo interpretar el gráfico de Agroal (pool de conexiones) en Grafana

Agroal es el pool de conexiones JDBC (Quarkus). Métricas Micrometer expuestas en
`/q/metrics` (Prometheus) → panel en Grafana:

- **`agroal_active_count`** — conexiones en uso ahora. Si se pega al **máximo** (`max-size`,
  aquí 20) y se queda ahí durante el pico, el pool es el cuello de botella.
- **`agroal_available_count`** — conexiones libres. Si cae a **0** de forma sostenida, llegan
  más peticiones de las que la BD puede atender.
- **`agroal_awaiting_count`** — peticiones **esperando** una conexión. Debe ser ≈0. Si sube,
  se traduce en latencia (las verás en `http_req_duration`) y, si supera el
  `acquisition-timeout` (5s), en errores.
- **`agroal_max_used_count`** — pico histórico de uso; ayuda a dimensionar `max-size`.

Lectura típica durante `02-pico-apuestas`: como las apuestas van por la **cola Redis** y solo
el worker toca la BD (lotes de 20 == `max-size`), `agroal_active` debe mantenerse acotado y
`agroal_awaiting` cerca de 0 **aunque** los VUs suban a 1000. Si en cambio `awaiting` se
dispara con cada pico, revisa que el flujo de escritura siga siendo asíncrono y que el
tamaño de lote del worker no supere `max-size`.
