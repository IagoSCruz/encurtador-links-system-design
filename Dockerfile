# Dockerfile — Multi-stage build
# Stage 1: Build com Maven (não vai para a imagem final)
# Stage 2: Runtime com JRE mínimo (imagem ~200MB vs ~500MB single-stage)

# ── Stage 1: Build ────────────────────────────
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /build

# Copia pom.xml primeiro para aproveitar cache de layers do Docker
# (as dependências só são baixadas novamente se o pom.xml mudar)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Runtime ───────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Usuário não-root por segurança
RUN addgroup -S app && adduser -S app -G app
USER app

WORKDIR /app

# Copia apenas o JAR gerado
COPY --from=build /build/target/*.jar app.jar

# Porta exposta
EXPOSE 8080

# Health check para o Docker/Kubernetes
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Flags JVM otimizadas para containers
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
