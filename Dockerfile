# --- Stage 1: Build ---
FROM maven:3.9-amazoncorretto-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# --- Stage 2: Runtime ---
FROM amazoncorretto:17-alpine
WORKDIR /app

# Install fonts and dependencies for Apache POI (Excel export with Vietnamese + Emoji)
RUN apk add --no-cache \
    fontconfig \
    ttf-dejavu \
    && fc-cache -f

COPY --from=builder /app/target/*.jar app.jar

# Cấu hình runtime
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

