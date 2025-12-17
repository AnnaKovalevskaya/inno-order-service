FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/order-service-1.0-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=docker"]