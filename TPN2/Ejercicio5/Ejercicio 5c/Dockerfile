FROM eclipse-temurin:24-jdk

ENV SERVER_PORT=8080
ENV SPRING_PROFILES_ACTIVE=prod
ENV JWT_SECRET=change-me-in-production
ENV JWT_EXPIRATION_MS=3600000

ARG JAR_FILE=target/jwt-parte-a-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
