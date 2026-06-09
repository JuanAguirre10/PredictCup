# ---------- Stage 1: build ----------
FROM node:20-alpine AS builder
WORKDIR /app

# Instala dependencias con el lockfile (reproducible).
COPY package.json package-lock.json ./
RUN npm ci

# Compila la app (output: 'standalone' en next.config.ts).
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
# URL publica del backend (se hornea en el bundle del cliente). Detras de Nginx
# el navegador llama al mismo origen: https://localhost -> /api y /ws.
ARG NEXT_PUBLIC_API_URL=https://localhost
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
# Muestra el dev-login en /login solo cuando se activa.
ARG NEXT_PUBLIC_DEV_LOGIN=false
ENV NEXT_PUBLIC_DEV_LOGIN=$NEXT_PUBLIC_DEV_LOGIN
# Base de las imagenes de bandera (carpeta local /flags o un CDN/R2).
ARG NEXT_PUBLIC_FLAGS_BASE=/flags
ENV NEXT_PUBLIC_FLAGS_BASE=$NEXT_PUBLIC_FLAGS_BASE
# Extension de los archivos de bandera (jpg por defecto).
ARG NEXT_PUBLIC_FLAGS_EXT=jpg
ENV NEXT_PUBLIC_FLAGS_EXT=$NEXT_PUBLIC_FLAGS_EXT
RUN npm run build

# ---------- Stage 2: runtime ----------
FROM node:20-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

# Usuario no root.
RUN addgroup -g 1001 -S nodejs && adduser -S nextjs -u 1001

# Solo lo necesario del build standalone.
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
CMD ["node", "server.js"]
