#
# Build
#
FROM maven:3.9.9-amazoncorretto-21-al2023@sha256:281aeb7254dde09226bde36c2ce236ead71c6451298986a79fa542bc26444225 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:21-alpine3.20@sha256:ca05e809506b30f75a5b90f766b177e3ae996ac606b63499b4bd75b1b7890451 AS runtime

RUN apk --no-cache add shadow
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.1/applicationinsights-agent-3.7.1.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
