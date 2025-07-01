# ┌── Stage 1: Build JAR with Maven ────────────────────────────────────────────
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache dependencies
# This step downloads all project dependencies, leveraging Docker's layer caching.
# If pom.xml doesn't change, this layer won't be rebuilt.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile Spring Boot fat JAR
# Copies source code and builds the executable JAR.
COPY src ./src
RUN mvn clean package -DskipTests -B


# ┌── Stage 2: Native-image Compilation ─────────────────────────────────────────
FROM ghcr.io/graalvm/native-image-community:21 AS native-builder
WORKDIR /workspace

# Copy the compiled JAR from the 'build' stage
COPY --from=build /workspace/target/bolt-track-*.jar app.jar

# Generate static native executable
# IMPORTANT: Added -J-Xmx8G to allocate 8GB of memory to the native-image build process.
# Adjust this value (e.g., 10G, 12G) if you still encounter OOM errors.
# The --no-fallback flag ensures a fully static native image is built.
# The --trace-class-initialization and --initialize-at-run-time flags help
# with Spring Boot specific initialization issues for native images.
# --enable-http and --enable-https ensure necessary networking components are included.
# -H:Name sets the name of the generated executable.
RUN native-image \
    -J-Xmx8G \
    --no-fallback \
    --trace-class-initialization=org.springframework.boot.loader.ref.DefaultCleaner \
    --initialize-at-run-time=org.springframework.boot.loader.net.protocol.jar.JarUrlConnection,org.springframework.boot.loader.ref.DefaultCleaner \
    --enable-http \
    --enable-https \
    -H:Name=bolt-track \
    -jar app.jar


# ┌── Stage 3: Runtime (scratch) ─────────────────────────────────────────────────
FROM scratch AS runtime
WORKDIR /app

# Copy the native binary from the 'native-builder' stage
# Using 'scratch' creates the smallest possible final image, containing only the executable.
COPY --from=native-builder /workspace/bolt-track /app/bolt-track

# Activate Spring 'prod' profile for production-specific configurations
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port for local testing (Cloud Run will handle port mapping automatically)
EXPOSE 8080

# Run the native Spring Boot application
ENTRYPOINT ["/app/bolt-track"]
