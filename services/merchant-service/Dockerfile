FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY pom.xml ./
RUN apk add --no-cache maven && mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests=true -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S swiftdrop && adduser -S swiftdrop -G swiftdrop
USER swiftdrop

COPY --from=builder /app/target/merchant-service-*.jar app.jar

ENV SERVER_PORT=8083
EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --retries=5 --start-period=20s \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
