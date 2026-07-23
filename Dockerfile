# ── Etapa 1: compilar con Maven ────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Descargar dependencias primero (aprovechar caché de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
# -Dmaven.test.skip=true omite compilación Y ejecución de tests (más robusto que -DskipTests)
RUN mvn package -Dmaven.test.skip=true -q

# ── Etapa 2: imagen de ejecución mínima ────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
