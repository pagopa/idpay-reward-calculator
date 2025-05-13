#
# Build
#
FROM maven:3.9.9-amazoncorretto-17-al2023@sha256:f594d808b6b36b797a9f58dfe67d6db9ed8bda93b70cc665a1a22c2222c6e853 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17-alpine3.20@sha256:4ad5194e923e28fb7298b473431a8445a5500a675e5be0a4ad223b8001b3051d AS runtime

RUN apk --no-cache add shadow
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.7.0/applicationinsights-agent-3.7.0.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
