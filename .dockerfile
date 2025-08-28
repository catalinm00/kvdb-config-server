# Etapa 1: build
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar pom.xml y resolver dependencias
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copiar el resto del código y compilar
COPY src ./src
RUN mvn package -DskipTests

# Etapa 2: runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar el JAR construido
COPY --from=builder /app/target/*.jar app.jar

# Puerto por defecto de Micronaut
EXPOSE 8080

# Ejecutar la aplicación
CMD ["java", "-jar", "app.jar"]
