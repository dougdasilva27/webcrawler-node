FROM openjdk:8-jre-slim

RUN apt-get update
RUN apt-get --no-install-recommends install chromium-driver -y

COPY target/deployment/webcrawler.jar /app/webcrawler.jar

COPY chromedriver/chromedriver /app/

WORKDIR /app

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "webcrawler.jar"]
