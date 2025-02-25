#
# Build
#
FROM maven:3.9.6-amazoncorretto-17-al2023@sha256:459be099faa25a32c06cd45ed1ef2bc9dbbf8a5414da4e72349459a1bb4d6166 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17-alpine3.20@sha256:38b44a49bfd11b925015b281d3d35c845daaeca5f0e3827f8e1df9bedf7b256a AS runtime

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
