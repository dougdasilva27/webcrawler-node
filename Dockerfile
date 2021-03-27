
FROM openjdk:8-jre-slim
MAINTAINER TEAM_DATA_CAPTURE (datacapture@lett.digital)
RUN apt-get update
RUN apt-get install -y maven

COPY pom.xml /home/app
COPY src /home/app/src
WORKDIR /app
RUN mvn package


EXPOSE 5000
CMD ["java", "-jar", "webcrawler.jar"]
