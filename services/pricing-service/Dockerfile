# Stage 1: Build JAR using Eclipse Temurin JDK 21
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy POM
COPY pom.xml ./
RUN apk add --no-cache maven && mvn dependency:go-offline -B

# Copy source code and build production package
COPY src ./src
RUN mvn clean package -DskipTests=true -B

# Stage 2: Minimal Runtime image using Eclipse Temurin JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S swiftdrop && adduser -S swiftdrop -G swiftdrop
USER swiftdrop

COPY --from=builder /app/target/pricing-service-*.jar app.jar

ENV SERVER_PORT=8085
EXPOSE 8085

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=20s \
  CMD wget -qO- http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
