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

# Copy source for modules we need to build
COPY common/src common/src
COPY retry-dispatcher/src retry-dispatcher/src

# Build common first (if dependency exists), then retry-dispatcher
RUN mvn clean install -pl common -am -DskipTests
RUN mvn clean package -pl retry-dispatcher -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/retry-dispatcher/target/retry-dispatcher-*.jar app.jar

# Expose the application port
EXPOSE 8082

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
