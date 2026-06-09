# Probar PredictCup en local (demo en vivo)

La demo trae: **admin por allowlist**, **dev-login sin Google**, **fixture sembrado**
(72 partidos de los 12 grupos) y un **simulador** que mueve los marcadores en vivo
(marcador → puntuación → ranking → notificaciones). Tu correo admin por defecto es
`juancho71096@gmail.com`.

---

## Opción A — Stack Docker completo (recomendado)

Levanta backend (3 réplicas) + frontend + Nginx (TLS) + Postgres + Redis + monitoreo,
ya con el modo demo activado por `.env`.

```bash
make ssl                       # cert autofirmado (una vez)
docker compose build app frontend   # reconstruir con los cambios de la demo
make prod                      # levanta el stack en segundo plano
```

Abre **https://localhost/login** (acepta el certificado autofirmado). Verás el botón
**“Login de desarrollo”** con tu correo precargado → **Entrar** → entras como **admin**.

Atajo directo (sin pasar por la página): abre
`https://localhost/auth/dev?email=juancho71096@gmail.com`.

---

## Opción B — Dev local (sin Docker, hot-reload)

Dos terminales:

```bash
# 1) Backend (perfil dev: dev-login + fixtures + sync 20s ya activos)
cd PredictCup && ./mvnw quarkus:dev

# 2) Frontend (activa el botón dev-login)
cd betting-frontend && NEXT_PUBLIC_DEV_LOGIN=true npm run dev
```

Abre **http://localhost:3000/login** → botón de dev-login. O directo:
`http://localhost:8080/auth/dev?email=juancho71096@gmail.com`.

---

## Qué probar (el flujo completo)

1. **Entra como admin** (dev-login con tu correo). Verás el enlace **Admin** en la navbar.
2. **Partidos** → hay ~55 con *apuestas abiertas*. Entra a uno y **apuesta** un marcador.
3. Espera ~1–4 min: el simulador hace que el partido pase a **EN_CURSO** (marcador subiendo)
   y luego **TERMINADO**. Al terminar se **calculan tus puntos**, se actualiza el **ranking
   en vivo** (WebSocket) y te llega una **notificación**.
4. **Ranking** y **Perfil** muestran tus puntos; el panel **Admin** tiene Partidos / Usuarios
   / Cola Redis.

> Ritmo de la demo: cada partido de 90′ se “juega” en `APP_DEMO_MATCH_DURATION_SECONDS`
> (240 s por defecto). Los kickoffs se escalonan ~50 s, así siempre hay partidos abriendo,
> jugándose y terminando. Ajusta `APP_DEMO_*` y `APP_SYNC_INTERVAL` en `.env`.

---

## Pasar a datos REALES (producción)

1. **Google OAuth**: crea credenciales en Google Cloud, redirect
   `https://localhost/auth/callback`. En `.env`: `QUARKUS_OIDC_ENABLED=true`,
   `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`. Desactiva el dev-login
   (`APP_AUTH_DEV_LOGIN_ENABLED=false`). Tu correo sigue siendo admin por la allowlist.
2. **API real de fútbol**: pon una key real en `FOOTBALL_API_KEY` (football-data.org). El
   `PartidoSyncJob` dejará de simular y consultará la API. *Pendiente*: un importador que
   cree los partidos reales con su `id_externo` (hoy el fixture es el sorteo ficticio del
   PDF; ver el spec de diseño).

---

## Foto de perfil y datos

En **/perfil** hay un lápiz para **editar el nombre** y, al editar, un ícono de cámara
sobre el avatar para **subir tu foto** (jpg/png/webp, ≤ 2 MB). El correo es de solo-lectura.

- **Local (por defecto):** la imagen se guarda en disco (volumen `avatars`, compartido por
  las 3 réplicas) y se sirve por `/api/usuarios/avatars/{archivo}`. La BD solo guarda la URL.
- **Cloudflare R2 (producción):** en `.env` pon `APP_STORAGE_AVATAR=r2` y completa
  `R2_ENDPOINT` (`https://<accountid>.r2.cloudflarestorage.com`), `R2_ACCESS_KEY`,
  `R2_SECRET_KEY`, `R2_BUCKET` y `R2_PUBLIC_URL` (dominio público del bucket). El mismo
  código sube a R2 y guarda la URL pública; el resto no cambia.

## Apagar

```bash
make down     # detiene el stack (make clean borra también los volúmenes)
```
