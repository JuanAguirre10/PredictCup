# ============================================================
# Stage 1 — Build (Maven + Temurin 21)
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache de dependencias: primero el pom, luego el codigo
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B -q dependency:go-offline || true

COPY src ./src
RUN mvn -B package -DskipTests

# ============================================================
# Stage 2 — Runtime (JRE 21 Alpine)
# ============================================================
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

# curl: lo usa el healthcheck de docker-compose. Usuario no-root.
RUN apk add --no-cache curl && addgroup -S app && adduser -S app -G app

# Quarkus fast-jar layout
COPY --from=build /app/target/quarkus-app/lib/      ./lib/
COPY --from=build /app/target/quarkus-app/*.jar     ./
COPY --from=build /app/target/quarkus-app/app/      ./app/
COPY --from=build /app/target/quarkus-app/quarkus/  ./quarkus/

USER app
EXPOSE 8080

# El bind a 0.0.0.0 va por config (no por JAVA_OPTS) para que compose pueda
# sobreescribir JAVA_OPTS con la memoria sin perder el host.
ENV QUARKUS_HTTP_HOST=0.0.0.0
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar quarkus-run.jar"]
