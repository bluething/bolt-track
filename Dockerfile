# ┌── GraalVM Build Stage ───────────────────────────────────────────────────────
FROM ghcr.io/graalvm/graalvm-ce:java21 AS builder

# 1) install native-image tool
RUN gu install native-image

WORKDIR /workspace

# 2) cache Maven deps
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 3) copy source & compile AOT + native image
COPY src ./src
# activates the 'native' profile
RUN mvn clean package -Pnative -DskipTests -B

# ┌── Runtime Stage ─────────────────────────────────────────────────────────────
# GraalVM‐produced binaries are fully static ⇒ we can use scratch
FROM scratch
# pick up the native binary
COPY --from=builder /workspace/target/bolt-track /app

# expose for tooling—but Cloud Run ignores this
EXPOSE 8080

ENTRYPOINT ["/app"]