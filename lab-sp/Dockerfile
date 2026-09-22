FROM maven:3.9.16-eclipse-temurin-21-noble AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package

FROM eclipse-temurin:21.0.12_8-jre-noble

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8000

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
