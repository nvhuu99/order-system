FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY target/auth-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
