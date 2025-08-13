# Etapa 1: Build con Gradle
FROM gradle:8.9-jdk17 AS builder

# Carpeta de trabajo dentro del contenedor
WORKDIR /home/gradle/project

# Copiamos solo los archivos de configuración primero (mejora cache de dependencias)
COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle gradle

# Descargamos dependencias (sin compilar todavía)
RUN ./gradlew dependencies --no-daemon || return 0

# Copiamos el resto del código
COPY . .

# Construimos el jar optimizado para producción
RUN ./gradlew clean build -x test --no-daemon

# Etapa 2: Imagen final ligera
FROM eclipse-temurin:17-jre-alpine

# Carpeta de trabajo
WORKDIR /app

# Copiamos el jar desde la etapa de build
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Variables de entorno (puedes cambiarlas en docker run)
ENV MICRONAUT_ENVIRONMENTS=prod \
    JAVA_OPTS="-Xms256m -Xmx512m"

# Puerto que expone la app (ajústalo según application.yml)
EXPOSE 8080

# Comando por defecto (pasa JAVA_OPTS si quieres tunear memoria, GC, etc.)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
