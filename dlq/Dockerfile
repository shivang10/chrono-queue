# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy parent pom
COPY pom.xml .

# Copy all module pom.xml files (Maven needs to see all declared modules)
COPY common/pom.xml common/
COPY job-producer/pom.xml job-producer/
COPY job-worker/pom.xml job-worker/
COPY retry-dispatcher/pom.xml retry-dispatcher/
COPY dlq/pom.xml dlq/

# Download dependencies (CACHED if POMs unchanged)
RUN ["mvn", "dependency:go-offline", "-pl", "dlq", "-am", "-B"]

# Copy source for modules we need to build
COPY common/src common/src
COPY dlq/src dlq/src

# Build (dependencies mostly cached, only compiles source)
RUN ["mvn", "clean", "package", "-pl", "dlq", "-am", "-DskipTests"]

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.source="https://github.com/shivang10/chrono-queue" \
    org.opencontainers.image.title="chrono-queue-dlq"

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# Copy the executable Spring Boot jar from build stage
COPY --from=build --chown=appuser:appgroup /app/dlq/target/dlq-0.0.1-SNAPSHOT.jar /app/app.jar

USER appuser

ENV SERVER_PORT=8083
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "/app/app.jar"]
