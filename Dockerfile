FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/RollercoasterBot-1.0-SNAPSHOT.jar ./bot.jar
ENTRYPOINT ["java", "-jar", "bot.jar"]
