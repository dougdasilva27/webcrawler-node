#
# Build stage
#
FROM maven:3.6.3-jdk-8-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package

#
# Package stage
#
FROM openjdk:8-jre-slim
COPY target/deployment/.jar /app/.jar
EXPOSE 5000
ENTRYPOINT ["java","-jar","/usr/local/lib/demo.jar"]
