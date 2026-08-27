FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /source
COPY pom.xml ./
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM docker:27-cli AS docker-cli

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=docker-cli /usr/local/bin/docker /usr/local/bin/docker
COPY --from=docker-cli /usr/local/libexec/docker/cli-plugins/docker-compose /usr/local/libexec/docker/cli-plugins/docker-compose
COPY --from=builder /source/target/*.jar /app/app.jar
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
