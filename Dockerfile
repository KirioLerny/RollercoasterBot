FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/target/RollercoasterBot-1.0-SNAPSHOT.jar ./bot.jar
ENTRYPOINT ["java", "-jar", "bot.jar"]