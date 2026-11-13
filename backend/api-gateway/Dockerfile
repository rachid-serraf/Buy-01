# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the application (skipping tests for speed)
RUN mvn clean package -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install openssl to generate the keystore at runtime using the .env SSL_PASSWORD
RUN apk add --no-cache openssl

# Copy the built JAR
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Generate edge-service.p12 using the runtime SSL_PASSWORD and start Spring Boot
ENTRYPOINT ["sh", "-c", "openssl req -x509 -newkey rsa:4096 -keyout /tmp/key.pem -out /tmp/cert.pem -sha256 -days 365 -nodes -subj '/C=MA/ST=Oujda/L=Oujda/O=Zone01/OU=IT/CN=localhost' && openssl pkcs12 -export -in /tmp/cert.pem -inkey /tmp/key.pem -out /app/edge-service.p12 -name springgateway -passout pass:${SSL_PASSWORD} && rm /tmp/key.pem /tmp/cert.pem && exec java -jar app.jar --server.ssl.key-store=file:/app/edge-service.p12"]
