# Imagen del servidor (E0-09). Multi-stage: se compila con Maven y se corre solo con el JRE.
# Se usa el Maven de la imagen (no ./mvnw) para no depender de los finales de línea de Windows.

# ---- Compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Primero solo el pom: las dependencias quedan en caché mientras no cambie
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Los tests los corre Jenkins (E1-09) antes de construir la imagen
RUN mvn -B -q package -DskipTests

# ---- Ejecución ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system dulcemayor && useradd --system --gid dulcemayor dulcemayor
COPY --from=build /app/target/*.jar app.jar
USER dulcemayor
ENV TZ=America/Argentina/Mendoza
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
