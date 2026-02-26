# Stage 1: Build
FROM gradle:jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

COPY src/ src/
RUN gradle bootJar --no-daemon -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
