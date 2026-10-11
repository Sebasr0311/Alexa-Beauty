# syntax=docker/dockerfile:1

# Stage 1: Build stage using official Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Install Maven in builder container
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

# Cache dependency layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and package application
COPY src src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre AS runner
WORKDIR /app

# Non-root user for container security hardening
RUN groupadd -r alexa && useradd -r -g alexa alexa

# Copy built artifact from builder stage
COPY --from=builder /workspace/target/*.jar app.jar
RUN chown -R alexa:alexa /app

USER alexa

ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
