# Stage 1: Build JAR using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper & POM
COPY pom.xml ./
# Download dependencies
RUN apk add --no-cache maven && mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests=true -B

# Stage 2: Minimal Runtime image using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S swiftdrop && adduser -S swiftdrop -G swiftdrop
USER swiftdrop

COPY --from=builder /app/target/identity-service-*.jar app.jar

ENV SERVER_PORT=8081
EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=20s \
  CMD wget -qO- http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
