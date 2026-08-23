# ── Etapa 1: compilar ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Las dependencias en su propia capa: mientras no toques el pom, Docker
# reutiliza esta capa y no vuelve a bajarse medio Maven Central.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Sin tests a proposito: los 1398 ya los corre el pipeline antes de llegar
# aqui, y ademas necesitan Docker para Testcontainers, que dentro de esta
# etapa no hay.
RUN mvn -B -q clean package -DskipTests

# ── Etapa 2: ejecutar ────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Nunca como root.
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/*.jar app.jar
USER app

EXPOSE 8080

# MaxRAMPercentage: la JVM mira la memoria del contenedor, no la del host.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
