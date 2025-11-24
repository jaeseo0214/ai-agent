# Dockerfile
FROM openjdk:dk-17.0.17.10-hotspot

WORKDIR /app

COPY . .

RUN ./gradlew build

CMD ["java", "-jar", "build/libs/aiAgent-1.0.0.jar"]
