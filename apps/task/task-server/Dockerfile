FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /source
COPY pom.xml ./
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /source/target/*.jar /app/app.jar
VOLUME ["/workspace/input", "/workspace/output"]
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
