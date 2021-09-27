FROM adoptopenjdk/openjdk11:alpine-jre

RUN apk add --no-cache chromium chromium-chromedriver

COPY target/deployment/webcrawler.jar /app/webcrawler.jar

WORKDIR /app

EXPOSE 5000

ENTRYPOINT ["java", "-jar", "webcrawler.jar"]
