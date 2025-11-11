FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Copiar archivos del proyecto
COPY pom.xml .
COPY src ./src

# Instalar Maven y construir
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

# Ejecutar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]