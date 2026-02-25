# ── Build stage ────────────────────────────────────────────────────────────────
# Using the official Gradle image avoids needing gradlew + gradle-wrapper.jar
FROM gradle:8.8-jdk21 AS build
WORKDIR /app

# Copy root build files
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle gradle

# Copy module build files (cache-layer: only re-download deps when build files change)
COPY workforce-processing-service/build.gradle.kts workforce-processing-service/
COPY workforce-common/build.gradle.kts workforce-common/

# Resolve dependencies (cached layer)
RUN gradle :workforce-processing-service:dependencies --no-daemon -q 2>/dev/null || true

# Copy sources
COPY workforce-processing-service/src workforce-processing-service/src
COPY workforce-common/src workforce-common/src

# Build the fat jar
RUN gradle :workforce-processing-service:bootJar --no-daemon -q

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

COPY --from=build /app/workforce-processing-service/build/libs/*.jar app.jar

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
