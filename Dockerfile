# Multi-stage build for Spring Boot Kotlin application
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Set working directory
WORKDIR /app

# Copy Maven configuration files
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM maven:3.9.6-eclipse-temurin-21

# Create app user with home directory
RUN useradd -r -m -s /bin/false app

# Set working directory
WORKDIR /app

# Copy Maven configuration files first
COPY --from=builder /app/pom.xml .

# Pre-download dependencies as root to cache in layer
RUN mvn dependency:go-offline -B

# Copy the rest of the project from builder stage
COPY --from=builder /app/src ./src
COPY --from=builder /app/target ./target

# Create Maven home directory for app user and copy cached dependencies
RUN mkdir -p /home/app/.m2 && cp -r /root/.m2/repository /home/app/.m2/ && chown -R app:app /home/app/.m2

# Change ownership to app user
RUN chown -R app:app .

# Switch to app user
USER app

ENV OPENAI_API_KEY=
ENV ANTHROPIC_API_KEY=
ENV GOOGLE_MAPS_API_KEY=

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["mvn", "-Dmaven.test.skip=true", "spring-boot:run"]
