# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only pom.xml first and download dependencies.
# Docker caches this layer, so later builds are fast unless pom.xml changes.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now copy the source code and build the jar (tests skipped to keep the build fast)
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: run the jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Take only the finished jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Documentation only: the port the gateway listens on
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]