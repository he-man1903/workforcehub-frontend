# ── Build stage ────────────────────────────────────────────────────────────────
# Using the official Gradle image avoids needing gradlew + gradle-wrapper.jar
FROM gradle:8.8-jdk21 AS build
WORKDIR /app

# Copy root build files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

# Copy module build files (cache-layer: only re-download deps when build files change)
COPY workforce-gateway/build.gradle.kts workforce-gateway/


# Resolve dependencies (cached layer)
RUN gradle :workforce-gateway:dependencies --no-daemon -q 2>/dev/null || true

# Copy sources
COPY workforce-gateway/src workforce-gateway/src


# Build the fat jar
RUN gradle :workforce-gateway:bootJar --no-daemon -q

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

COPY --from=build /app/workforce-gateway/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
