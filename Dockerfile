FROM amazoncorretto:8

WORKDIR /app

COPY target/MusicAppAssignment-1.0-SNAPSHOT.jar app.jar

EXPOSE 80

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=80"]