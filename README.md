# 🏆 PredictCup — Sistema de Predicciones del Mundial 2026

Plataforma web de **predicciones deportivas** (sin dinero real) para el Mundial 2026. Los
usuarios predicen los marcadores de los partidos, ganan puntos según su acierto, compiten en
salas privadas y suben en un **ranking en vivo**. Diseñada para soportar **hasta 10 000
usuarios concurrentes** mediante una arquitectura escalable horizontalmente.

> Proyecto académico — TECSUP 2026. Solo uso educativo, sin apuestas con dinero real.

---

## 🔗 Enlaces

| Recurso | Enlace |
|---|---|
| 📂 **Repositorio (GitHub)** | https://github.com/JuanAguirre10/PredictCup |
| 🐳 **Imagen backend (Docker Hub)** | https://hub.docker.com/r/juan12211/predictcup-backend |
| 🐳 **Imagen frontend (Docker Hub)** | https://hub.docker.com/r/juan12211/predictcup-frontend |
| ▶️ **Video explicativo (YouTube)** | `https://youtu.be/TDuO0cjBxuM` |

> ⚠️ **Las imágenes de Docker Hub por sí solas NO bastan para levantar el sistema.** Son los
> dos servicios ya compilados (backend y frontend), pero **necesitan los archivos de
> configuración y orquestación que están en este repositorio de GitHub**: `docker-compose.yml`,
> la carpeta `nginx/` (config + certificado TLS), `monitoring/`, tu archivo `.env`, las llaves
> JWT y el certificado. En otras palabras: **GitHub + Docker Hub se complementan** — las
> imágenes son los programas, y el repo es la configuración para hacerlos funcionar juntos.
>
> Por eso, la forma recomendada de ejecutar el proyecto es **clonar este repositorio** y seguir
> los pasos de [Instalación](#-instalación-y-configuración) y [Cómo ejecutar](#-cómo-ejecutar).

---

## 📑 Tabla de contenidos

1. [Características](#-características)
2. [Stack tecnológico](#-stack-tecnológico)
3. [Arquitectura](#-arquitectura)
4. [Estructura del proyecto](#-estructura-del-proyecto)
5. [Requisitos previos](#-requisitos-previos)
6. [Instalación y configuración](#-instalación-y-configuración)
7. [Cómo ejecutar](#-cómo-ejecutar)
8. [Variables de entorno](#-variables-de-entorno)
9. [Uso de la aplicación](#-uso-de-la-aplicación)
10. [Sistema de puntuación](#-sistema-de-puntuación)
11. [Banderas de los países](#-banderas-de-los-países)
12. [Pruebas](#-pruebas)
13. [Escalamiento horizontal](#-escalamiento-horizontal)
14. [Solución de problemas](#-solución-de-problemas)

---

## ✨ Características

- 🔐 **Login con Google** (OAuth 2.0) + JWT propio.
- ⚽ **Predicciones** de marcadores con validación de ventana de apuesta.
- 🏅 **Sistema de puntuación** con 5 reglas (exacto, ganador, diferencia + bonos).
- 📊 **Ranking global y por salas** en tiempo real (WebSocket).
- 👥 **Salas privadas** con código de invitación.
- 🥇 **Fase de grupos + eliminatorias** (bracket de 16avos a la final).
- 👤 **Perfil editable** (nombre + foto de perfil).
- 🌓 **Tema claro / oscuro** y diseño responsive.
- 🛠️ **Panel de administración** (resultados manuales, gestión del bracket, usuarios).
- 🎮 **Modo demo** con simulador de partidos en vivo para probar sin API real.
- 📈 **Observabilidad** (Prometheus + Grafana) y **pruebas de estrés** (k6).

---

## 🧰 Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | **Java 21 · Quarkus 3.36** (JAX-RS, Panache, CDI) |
| Frontend | **Next.js 16** (App Router, TypeScript, Tailwind CSS v4) |
| Base de datos | **PostgreSQL 16** (datos) + **Redis 7** (cola, rankings, locks, pub/sub) |
| Autenticación | Google OAuth 2.0 (flujo *code*) + **JWT RS256** propio |
| Migraciones | **Flyway** |
| Tiempo real | WebSocket + Redis Pub/Sub |
| Balanceo / TLS | **Nginx** |
| Empaquetado | **Docker** + Docker Compose |
| Monitoreo | Micrometer + Prometheus + Grafana |
| Pruebas de carga | **k6** |

---

## 🏗️ Arquitectura

Arquitectura **por capas y por funcionalidad** (*layered by feature*): cada dominio
(`apuesta`, `partido`, `sala`, `usuario`, `puntuacion`) es un paquete autocontenido. El
backend es **sin estado**, lo que permite **escalar horizontalmente** detrás de Nginx,
apoyándose en Redis y PostgreSQL como estado compartido.

> 📌 Diagrama completo en [`Arquitectura.png`](Arquitectura.png) y modelo de datos en [`E-R.png`](E-R.png).

**Flujo crítico (apuestas asíncronas):** `POST /api/apuestas` → rate-limit → encola en
Redis → responde `202` inmediato → un *worker* consume por lotes (`BRPOP`) y persiste de
forma idempotente → al terminar el partido se calculan los puntos y se difunde el ranking
por WebSocket (vía Pub/Sub, válido entre las réplicas).

---

## 📁 Estructura del proyecto

```
Apuesta/
├── PredictCup/              # Backend Quarkus (Java 21)
│   ├── src/main/java/com/mundial/
│   │   ├── apuesta/         # Predicciones y puntuación
│   │   ├── partido/         # Partidos, fases, bracket, sincronización
│   │   ├── sala/            # Salas privadas
│   │   ├── usuario/         # Perfil, ranking, auth, admin
│   │   ├── puntuacion/      # ScoringService (lógica pura)
│   │   └── infrastructure/  # Redis, cola, WebSocket, API externa, filtros
│   ├── src/main/resources/db/migration/   # Migraciones Flyway (V1..V7)
│   └── Dockerfile
├── betting-frontend/        # Frontend Next.js 16
│   ├── src/app/             # Páginas (partidos, grupos, eliminatorias, ranking, …)
│   ├── src/components/      # Componentes reutilizables
│   └── public/flags/        # Banderas de los países (.jpg)
├── nginx/                   # Configuración de Nginx (TLS + balanceo)
├── monitoring/              # Prometheus + Grafana
├── stress-tests/            # Pruebas de carga con k6
├── docker-compose.yml       # Stack de producción (3 réplicas + monitoreo)
├── docker-compose.dev.yml   # Override para desarrollo (1 réplica)
└── Makefile                 # Atajos de comandos
```

---

## ✅ Requisitos previos

- **Docker Desktop** (con Docker Compose) — **obligatorio** (incluso en modo desarrollo, ya
  que Quarkus levanta PostgreSQL y Redis automáticamente vía *Dev Services*).
- **JDK 21** (Temurin) — solo si vas a correr el backend en modo desarrollo.
- **Node.js 20+** — solo si vas a correr el frontend en modo desarrollo.
- **OpenSSL** — para generar las llaves JWT y el certificado TLS.
- Una **cuenta de Google Cloud** — para las credenciales OAuth (gratis).

---

## ⚙️ Instalación y configuración

### 1. Clonar el repositorio

```bash
git clone https://github.com/____________________.git
cd Apuesta
```

### 2. Generar las llaves JWT (firma de tokens)

Por seguridad, las llaves **no** se incluyen en el repositorio. Genéralas dentro de los
recursos del backend:

```bash
cd PredictCup/src/main/resources
openssl genrsa -out privateKey.pem 2048
openssl rsa -in privateKey.pem -pubout -out publicKey.pem
cd ../../../..
```

### 3. Crear las credenciales de Google OAuth

1. Entra a <https://console.cloud.google.com/> y crea un proyecto.
2. **APIs y servicios → Pantalla de consentimiento de OAuth**: tipo **Externo**, completa el
   nombre y, en **Usuarios de prueba**, añade tu correo de Google.
3. **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth → Aplicación web**.
4. En **URIs de redireccionamiento autorizados** añade, según cómo lo vayas a correr:
   - Desarrollo local: `http://localhost:8080/auth/oidc`
   - Docker (producción): `https://localhost/auth/oidc`
5. Copia el **Client ID** y el **Client Secret** (los usarás en el `.env`).

### 4. Configurar las variables de entorno

```bash
cp .env.example .env
```

Edita `.env` y coloca, como mínimo:
- `GOOGLE_CLIENT_ID` y `GOOGLE_CLIENT_SECRET` (del paso anterior).
- Cambia las contraseñas (`POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `GRAFANA_PASSWORD`).
- En `PredictCup/.../UsuarioService` el rol admin se asigna por *allowlist*: define tu correo
  con la variable `APP_ADMIN_EMAILS=tu-correo@gmail.com`.

> **Para modo desarrollo** (sin Docker Compose) crea además `PredictCup/.env` con:
> ```
> GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
> GOOGLE_CLIENT_SECRET=tu-secreto
> APP_ADMIN_EMAILS=tu-correo@gmail.com
> ```

### 5. Generar el certificado TLS (solo para Docker)

```bash
make ssl          # genera nginx/ssl/cert.pem y key.pem (autofirmado)
```

---

## ▶️ Cómo ejecutar

Hay **dos formas** de levantar el proyecto.

### Opción A — Desarrollo local (recomendada para programar)

Backend y frontend por separado; Quarkus levanta PostgreSQL y Redis automáticamente (requiere
Docker corriendo). Incluye **modo demo** (siembra partidos y simula marcadores en vivo).

```bash
# Terminal 1 — backend (http://localhost:8080)
cd PredictCup
./mvnw quarkus:dev

# Terminal 2 — frontend (http://localhost:3000)
cd betting-frontend
npm install
npm run dev
```

Abre **<http://localhost:3000>** e inicia sesión con Google.

### Opción B — Stack completo con Docker Compose (producción)

Levanta Nginx (TLS + balanceo), **3 réplicas** del backend, frontend, PostgreSQL, Redis,
Prometheus y Grafana, con un solo comando:

```bash
make prod          # = docker compose up -d   (segundo plano)
```

Servicios expuestos:
- Aplicación: **<https://localhost>** (acepta el certificado autofirmado)
- Grafana: **<http://localhost:3001>** (usuario `admin`, contraseña del `.env`)

Otros comandos útiles (Makefile):

```bash
make dev           # stack en 1 réplica, con logs en consola
make scale N=5     # escala el backend a 5 réplicas
make logs          # sigue los logs del backend
make build         # construye las imágenes
make down          # detiene (conserva los datos / volúmenes)
make clean         # detiene y BORRA los volúmenes (datos)
make stress        # ejecuta las pruebas de estrés k6
```

---

## 🔧 Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credenciales de PostgreSQL | `mundial` / `secreto` |
| `DATABASE_URL` | URL JDBC del backend | `jdbc:postgresql://postgres:5432/mundial_apuestas` |
| `REDIS_URL` / `REDIS_PASSWORD` | Conexión a Redis | `redis://:pass@redis:6379` |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Credenciales OAuth de Google | — |
| `APP_ADMIN_EMAILS` | Correos con rol **admin** (separados por coma) | `tu@gmail.com` |
| `JWT_ISSUER` | Emisor del JWT (debe coincidir backend/verificación) | `https://localhost` |
| `FRONTEND_URL` | A dónde redirige tras el login | `https://localhost/login/callback` |
| `FOOTBALL_API_KEY` | `demo` = simulador; clave real = football-data.org | `demo` |
| `NEXT_PUBLIC_API_URL` | URL del backend para el frontend | `https://localhost` |
| `NEXT_PUBLIC_FLAGS_EXT` | Extensión de las banderas | `jpg` |
| `GRAFANA_PASSWORD` | Contraseña de admin de Grafana | `admin123` |

---

## 🕹️ Uso de la aplicación

1. **Inicia sesión** con Google. Tu cuenta se crea automáticamente; si tu correo está en
   `APP_ADMIN_EMAILS`, entras como **administrador**.
2. **Partidos**: filtra por día (← →) y entra a un partido para **predecir el marcador**
   (hasta 10 minutos antes del inicio).
3. **Grupos**: tablas de posiciones por grupo, calculadas automáticamente.
4. **Eliminatorias**: cuadro de 16avos → final (predecibles igual que los de grupos).
5. **Ranking**: clasificación global en vivo.
6. **Salas**: crea una sala privada y comparte su código para competir con amigos.
7. **Mi perfil**: edita tu nombre y sube tu foto.
8. **Mis predicciones**: historial privado con tus puntos y aciertos.

### Panel de administración (`/admin`)

- **Partidos**: editar marcador y estado (plan de contingencia si la API falla en vivo).
- **Eliminatorias**: *Generar bracket vacío* y asignar países + fecha a cada cruce.
- **Usuarios**: cambiar roles y activar/desactivar.
- **Cola Redis**: monitoreo de la cola de apuestas.

---

## 🎯 Sistema de puntuación

Por cada predicción se aplica **una sola** categoría base, más los bonos:

| Regla | Puntos | Descripción |
|---|:---:|---|
| **Resultado exacto** | 5 | Acierta el marcador exacto (predice 2-1, termina 2-1). |
| **Ganador correcto** | 3 | Acierta quién gana o el empate (predice 1-0, termina 3-1). |
| **Diferencia de goles** | 2 | Acierta el margen (predice 3-1, termina 2-0). |
| **Bonus por racha** | +2 | Por cada 3 partidos consecutivos acertados. |
| **Predicción anticipada** | +1 | Si se registra con más de 24 h de anticipación. |

`total = base(5/3/2/0) + bonos`. Las mismas reglas aplican a grupos y eliminatorias.

---

## 🚩 Banderas de los países

El componente `<Bandera>` busca cada imagen en `betting-frontend/public/flags/{codigo}.jpg`
(código FIFA en minúscula). Si falta, cae automáticamente al emoji. Para añadirlas, copia tus
imágenes ahí:

```
betting-frontend/public/flags/bra.jpg   usa.jpg   arg.jpg   ...
```

> Nota: Haití usa el archivo `hai.jpg` (su código FIFA). La extensión se cambia con
> `NEXT_PUBLIC_FLAGS_EXT` y la ubicación con `NEXT_PUBLIC_FLAGS_BASE` (por si usas un CDN/R2).

---

## 🧪 Pruebas

### Pruebas automatizadas del backend (42 pruebas)

```bash
cd PredictCup
./mvnw test
```

Incluye: unitarias del motor de puntuación (JUnit), integración con `@QuarkusTest` +
Testcontainers/H2, y de endpoint con REST-assured.

### Pruebas de estrés (k6)

Con el stack levantado (`make prod`):

```bash
make stress                         # ejecuta los 4 escenarios
# o individualmente:
k6 run stress-tests/01-carga-normal.js
k6 run stress-tests/02-pico-apuestas.js
k6 run stress-tests/03-websocket.js
k6 run stress-tests/04-sistema-completo.js
```

| Escenario | Carga | Umbral |
|---|---|---|
| Carga normal | 500 VUs · 2 min | p95 < 200 ms, < 2 % errores |
| Pico de apuestas | 1000 VUs | 0 errores `500`, p95 < 2 s |
| WebSocket | 300 conexiones | handshake p95 < 1 s |
| Sistema completo | hasta 1000 VUs | p95 < 500 ms |

---

## 📈 Escalamiento horizontal

El backend es **sin estado** (auth por JWT, estado compartido en Redis/PostgreSQL), por lo que
se escala añadiendo réplicas detrás de Nginx:

```bash
make scale N=5      # 5 réplicas del backend
```

Coordinación entre réplicas: la **cola Redis** (`BRPOP`) reparte el trabajo, los **rankings**
viven en *sorted sets* de Redis y la **difusión WebSocket** se hace por **Redis Pub/Sub** (así
funciona sin importar a qué réplica esté conectado cada usuario).

---

## 🆘 Solución de problemas

| Problema | Solución |
|---|---|
| `redirect_uri_mismatch` al entrar con Google | La URI en Google Cloud debe ser **exacta**: `http://localhost:8080/auth/oidc` (dev) o `https://localhost/auth/oidc` (Docker). |
| "Google no verificó esta app" | Es normal en modo *prueba*: añade tu correo a **Usuarios de prueba** y pulsa *Continuar*. |
| El backend no arranca por las llaves | Genera `privateKey.pem` y `publicKey.pem` (paso 2 de instalación). |
| El navegador bloquea HTTPS local | El certificado es autofirmado: acéptalo manualmente en `https://localhost`. |
| Reinicié el backend (dev) y perdí la sesión / datos | En desarrollo la base de datos es efímera: vuelve a iniciar sesión. Dentro de una misma sesión los datos persisten. |
| Las banderas salen como emoji | Faltan los archivos `.jpg` en `public/flags/` (ver sección de banderas). |

---

## 📝 Licencia y aviso

Proyecto **educativo** desarrollado para TECSUP 2026. **No** gestiona dinero real ni apuestas
reales; su único fin es académico y de demostración técnica.
