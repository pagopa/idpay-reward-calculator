#
# Build
#
FROM maven:3.9.3-amazoncorretto-17@sha256:1d4c323f286a39da2c0930ab42115b04b4d7efd40710273e38f6e80f28069753 as buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17.0.8-alpine3.18@sha256:f59b4f511346db4e473fb98c65b86254926061ce2b398295e975d0632fa4e2fd as runtime

RUN apk add shadow
RUN apk upgrade libssl3 libcrypto3
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.15/applicationinsights-agent-3.4.15.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
