FROM eclipse-temurin:21-jdk
ARG JAR_FILE=target/ecommerce-0.0.1-SNAPSHOT.jar
WORKDIR /app
COPY ${JAR_FILE} app_zero.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app_zero.jar"]