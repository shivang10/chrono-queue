# Multi-stage build for common library and all services
# Usage: docker build --target <service> -t chrono-queue-<service> .
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy root pom
COPY pom.xml .

# Copy all module poms
COPY common/pom.xml common/
COPY job-producer/pom.xml job-producer/
COPY job-worker/pom.xml job-worker/
COPY retry-dispatcher/pom.xml retry-dispatcher/
COPY dlq/pom.xml dlq/

# Download dependencies (CACHED if POMs unchanged)
RUN ["mvn", "dependency:go-offline", "-B"]

# Copy source code
COPY common/src common/src
COPY job-producer/src job-producer/src
COPY job-worker/src job-worker/src
COPY retry-dispatcher/src retry-dispatcher/src
COPY dlq/src dlq/src

# Build all modules (dependencies mostly cached, only compiles source)
RUN ["mvn", "clean", "package", "-DskipTests"]

# ── Shared runtime base ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime-base

LABEL org.opencontainers.image.source="https://github.com/shivang10/chrono-queue"

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

# ── Job Producer Service ─────────────────────────────────────────────
FROM runtime-base AS job-producer

LABEL org.opencontainers.image.title="chrono-queue-job-producer"

COPY --from=build --chown=appuser:appgroup /app/job-producer/target/job-producer-0.0.1-SNAPSHOT.jar /app/app.jar

USER appuser

ENV SERVER_PORT=8080
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "/app/app.jar"]

# ── Job Worker Service ───────────────────────────────────────────────
FROM runtime-base AS job-worker

LABEL org.opencontainers.image.title="chrono-queue-job-worker"

COPY --from=build --chown=appuser:appgroup /app/job-worker/target/job-worker-0.0.1-SNAPSHOT.jar /app/app.jar

USER appuser

ENV SERVER_PORT=8081
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "/app/app.jar"]

# ── Retry Dispatcher Service ─────────────────────────────────────────
FROM runtime-base AS retry-dispatcher

LABEL org.opencontainers.image.title="chrono-queue-retry-dispatcher"

COPY --from=build --chown=appuser:appgroup /app/retry-dispatcher/target/retry-dispatcher-0.0.1-SNAPSHOT.jar /app/app.jar

USER appuser

ENV SERVER_PORT=8082
EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "/app/app.jar"]

# ── DLQ Service ──────────────────────────────────────────────────────
FROM runtime-base AS dlq

LABEL org.opencontainers.image.title="chrono-queue-dlq"

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
