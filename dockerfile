FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Instalar Maven primero
RUN apk add --no-cache maven

# Copiar pom.xml y descargar dependencias (cache eficiente)
COPY pom.xml ./
RUN mvn dependency:resolve

# Copiar el código fuente
COPY src ./src

# Construir la aplicación
RUN mvn clean package -DskipTests

# Exponer puerto y ejecutar la app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/backend-0.0.1-SNAPSHOT.jar"]