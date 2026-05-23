# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

# Stage 2: Run (much smaller image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx256m", \
  "-jar", "app.jar"]