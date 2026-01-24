# Multi-stage build for common library and all services
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copy root pom
COPY pom.xml .

# Copy all module poms
COPY common/pom.xml common/
COPY job-producer/pom.xml job-producer/
COPY job-worker/pom.xml job-worker/
COPY retry-dispatcher/pom.xml retry-dispatcher/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY common/src common/src
COPY job-producer/src job-producer/src
COPY job-worker/src job-worker/src
COPY retry-dispatcher/src retry-dispatcher/src

# Build all modules
RUN mvn clean package -DskipTests

# Job Producer Service
FROM eclipse-temurin:21-jre-alpine AS job-producer
WORKDIR /app
COPY --from=build /app/job-producer/target/job-producer-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Job Worker Service
FROM eclipse-temurin:21-jre-alpine AS job-worker
WORKDIR /app
COPY --from=build /app/job-worker/target/job-worker-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# Retry Dispatcher Service
FROM eclipse-temurin:21-jre-alpine AS retry-dispatcher
WORKDIR /app
COPY --from=build /app/retry-dispatcher/target/retry-dispatcher-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
