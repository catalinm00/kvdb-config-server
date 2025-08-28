# Stage 1: Build
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom and resolve dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the fat JAR - Maven Shade creates {artifactId}-{version}.jar
COPY --from=builder /app/target/config-server-0.1.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]