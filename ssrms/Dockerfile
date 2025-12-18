FROM eclipse-temurin:21-jre

WORKDIR /app

LABEL authors="cloudby"

COPY target/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","/app/app.jar"]