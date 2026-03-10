FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/auth-center-3.3.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
