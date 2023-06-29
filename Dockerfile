# syntax=docker/dockerfile:1
FROM openjdk:17-alpine as builder

RUN mkdir -p /usr/local/search-profile-service

WORKDIR /usr/local/search-profile-service

# copies everything not excluded from the .dockerignore into base image WORKDIR
COPY . .

# package application and generate an executable .jar file
RUN ./mvnw clean package spring-boot:repackage -Dtest=\!*ContainerizedTest

# actual image that is used by the application
FROM openjdk:17-alpine as dev

RUN mkdir -p /usr/local/search-profile-service
RUN adduser -Ds /bin/sh local
RUN chown local:local /usr/local/search-profile-service

USER local

# copies executable jar from builder container
COPY --chown=local:local --from=builder /usr/local/search-profile-service/target/search-profile-service-0.0.1-SNAPSHOT.jar /usr/local/search-profile-service

WORKDIR /usr/local/search-profile-service

EXPOSE 7080

ENTRYPOINT ["java", "-jar", "search-profile-service-0.0.1-SNAPSHOT.jar"]