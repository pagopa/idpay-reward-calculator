#
# Build
#
FROM maven:3.9.1-amazoncorretto-17 as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17.0.7-alpine3.17@sha256:a1d3de7e050b7b6d9adee4d844afc2d1c5bde7e3c81641caffd3038b910997c9 as runtime

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.11/applicationinsights-agent-3.4.11.jar /app/applicationinsights-agent.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]
