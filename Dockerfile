#
# Build
#
FROM maven:3.9.12-amazoncorretto-25-alpine@sha256:0437187207c8466d4efb733230acc67b7de72f702dbcd89500c843018d887072 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:25-alpine3.22@sha256:3ffb0afccd262c33a0ae14f2fdde129eb44d18de9c4288379f9c3eeb701af5a8 AS runtime

RUN apk --no-cache add shadow \
&& useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.7/applicationinsights-agent-3.7.7.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
