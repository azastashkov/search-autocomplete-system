FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY gradlew .
COPY common common
COPY load-test-client load-test-client

RUN ./gradlew :load-test-client:bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/load-test-client/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
