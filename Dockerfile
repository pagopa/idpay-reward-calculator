#
# Build
#
FROM maven:3.9.4-amazoncorretto-17-al2023@sha256:c7719f952f62e301c6c24b86ef9a2ea1cd0a314a862ed12e51f0ffbc3fbb96b5 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17.0.8-alpine3.18@sha256:34650d7c653af234dad21cd2d89d2f0dbdb1bad54041014932e51b3492e0dec5 AS runtime

RUN apk add shadow
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.16/applicationinsights-agent-3.4.16.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
