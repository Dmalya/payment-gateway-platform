# Stage 1: Build and extract layers
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace/app

COPY pom.xml .

# Fetch dependencies (cached layer)
RUN mvn dependency:go-offline

# Copy source code and build
COPY src src
RUN mvn clean package -DskipTests

# Extract layers into a named folder
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# Stage 2: Create the optimized runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy extracted layers from named folder
COPY --from=builder /workspace/app/extracted/dependencies/ ./
COPY --from=builder /workspace/app/extracted/spring-boot-loader/ ./
COPY --from=builder /workspace/app/extracted/snapshot-dependencies/ ./
COPY --from=builder /workspace/app/extracted/application/ ./

# Expose Gateway Port
EXPOSE 8085

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]