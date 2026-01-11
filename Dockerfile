FROM gradle:8.10-jdk21 AS build
WORKDIR /home/gradle/project
COPY . .
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/project/build/libs/*.jar /app/app.jar
EXPOSE 9200
ENTRYPOINT ["java","-jar","/app/app.jar"]
