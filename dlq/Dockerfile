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
RUN mvn dependency:go-offline -pl dlq -am -B

# Copy source for modules we need to build
COPY common/src common/src
COPY dlq/src dlq/src

# Build (dependencies mostly cached, only compiles source)
RUN mvn clean package -pl dlq -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/dlq/target/dlq-*.jar app.jar

# Expose the application port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
