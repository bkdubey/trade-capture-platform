FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml
RUN mvn -B -ntp -pl backend -am dependency:go-offline
COPY backend/src backend/src
RUN mvn -B -ntp -pl backend -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/backend/target/trade-capture-api-1.0.0-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
