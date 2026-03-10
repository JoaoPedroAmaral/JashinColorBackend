# Stage 1: Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app
COPY pom.xml .
# Download dependencies first (cached)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install system libraries required for AWT/PDF processing in headless mode
RUN apt-get update && apt-get install -y \
    libfontconfig1 \
    libfreetype6 \
    && rm -rf /var/lib/apt/lists/*

# Import Aiven SSL Certificate into Java TrustStore
COPY ca.pem /app/ca.pem
RUN keytool -importcert -alias aiven-ca -file /app/ca.pem -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt

COPY --from=build /app/target/*.jar app.jar

# Render uses the PORT environment variable
ENV PORT=8080
EXPOSE ${PORT}

# Optimization for Render and Java Headless mode
ENTRYPOINT ["java", \
    "-Djava.awt.headless=true", \
    "-Dserver.port=${PORT}", \
    "-Xmx384m", \
    "-jar", "app.jar"]
