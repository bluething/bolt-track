# ┌── GraalVM Build Stage ───────────────────────────────────────────────────────
FROM ghcr.io/graalvm/jdk-community:21 AS builder

# Install Maven
RUN microdnf update -y && \
    microdnf install -y wget tar gzip && \
    wget https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz && \
    tar -xzf apache-maven-3.9.6-bin.tar.gz -C /opt && \
    ln -s /opt/apache-maven-3.9.6 /opt/maven && \
    rm apache-maven-3.9.6-bin.tar.gz && \
    microdnf clean all

ENV PATH="/opt/maven/bin:${PATH}"
ENV MAVEN_HOME="/opt/maven"

WORKDIR /workspace

# Verify installations
RUN java -version && mvn -version

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build native image
COPY src ./src
RUN mvn clean package -Pnative -DskipTests -B

# ┌── Runtime Stage ─────────────────────────────────────────────────────────────
FROM scratch

# Copy the native binary
COPY --from=builder /workspace/target/bolt-track.jar /app

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port
EXPOSE 8080

ENTRYPOINT ["/app"]