FROM openjdk:8-jre-slim

COPY target/deployment/webcrawler.jar /app/webcrawler.jar

WORKDIR /app

EXPOSE 5000

CMD ["java", "-jar", "webcrawler.jar"]
