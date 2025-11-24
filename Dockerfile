# Dockerfile
FROM gradle:jdk17-jammy AS build

WORKDIR /app

COPY . .

RUN gradle build --no-daemon

CMD ["java", "-jar", "build/libs/aiAgent-1.0.0.jar"]
