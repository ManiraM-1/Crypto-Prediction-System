# -----------------------------------------------------------------------------
# STEP 1: Build the Spring Boot Application
# -----------------------------------------------------------------------------
# FIXED: Using a Maven image that supports Java 21
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy the uploaded files into the builder
COPY . .

# Build the JAR file (skipping tests to speed it up)
RUN mvn clean package -DskipTests

# -----------------------------------------------------------------------------
# STEP 2: Run the Application
# -----------------------------------------------------------------------------
# FIXED: Using a Java 21 runtime image
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the compiled JAR from the build step
COPY --from=build /app/target/*.jar app.jar

# Hugging Face Spaces listens on port 7860
EXPOSE 7860

# Run the app and FORCE it to use port 7860
ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=7860"]