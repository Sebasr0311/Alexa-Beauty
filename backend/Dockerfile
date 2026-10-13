# syntax=docker/dockerfile:1

# Stage 1: Build stage using official Maven with Eclipse Temurin JDK 21
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Configure Google Cloud Maven Central Mirror to prevent HTTP 429 rate limits on cloud runners
COPY settings.xml /root/.m2/settings.xml

# Copy POM and source code, package application
COPY pom.xml .
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

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Xss512k", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
