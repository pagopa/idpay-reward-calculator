#
# Build
#
FROM maven:3.8.4-openjdk-17-slim as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package

#
# Docker RUNTIME
#
FROM eclipse-temurin:17-jre-alpine as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.3.0/applicationinsights-agent-3.3.0.jar /app/applicationinsights-agent.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
