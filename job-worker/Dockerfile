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
COPY job-worker/src job-worker/src

# Build common first, then job-worker
RUN mvn clean install -pl common -am -DskipTests
RUN mvn clean package -pl job-worker -am -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/job-worker/target/job-worker-*.jar app.jar

# Expose the application port
EXPOSE 8081

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
