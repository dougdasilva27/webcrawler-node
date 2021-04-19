FROM openjdk:8-jre-slim

COPY target/deployment/*.jar /app/webcrawler.jar
WORKDIR /app

CMD ["java", "-jar", "webcrawler.jar"]
