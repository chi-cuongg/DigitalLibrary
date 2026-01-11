# Multi-stage build để giảm kích thước image

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml và download dependencies (cache layer)
COPY demo/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY demo/src ./src
COPY demo/pom.xml .

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Create directories
RUN mkdir -p uploads logs

# Copy jar from build stage
COPY --from=build /app/target/demo-*.jar app.jar

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
RUN chown -R spring:spring /app
USER spring:spring

# Expose port
EXPOSE 8080

# Healthcheck
# HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
#   CMD curl -f http://localhost:8080/ || exit 1

# Run application
ENTRYPOINT ["java", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-jar", \
  "app.jar"]
