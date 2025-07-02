### Multi-stage build for smallest possible image (Java 21, non-native)

# ┌── Build Stage ─────────────────────────────────────────────────
FROM maven:3.9.10-eclipse-temurin-21-alpine AS builder

# Set working directory
WORKDIR /workspace

# Copy Maven configuration and source
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for speed)
RUN mvn clean package -DskipTests -B

# ┌── Runtime Stage ────────────────────────────────────────────────
FROM gcr.io/distroless/java21-debian12:nonroot

# Create app directory
WORKDIR /app

# Copy the runnable JAR
COPY --from=builder /workspace/target/bolt-track.jar ./bolt-track.jar

# Activate production profile
ENV SPRING_PROFILES_ACTIVE=prod

# Expose application port
EXPOSE 8080

# Launch the Spring Boot application
ENTRYPOINT ["java", "-jar", "./bolt-track.jar"]