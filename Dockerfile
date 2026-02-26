FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
COPY build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
