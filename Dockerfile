#
# Build
#
FROM maven:3.9.6-amazoncorretto-17-al2023@sha256:9ace9c9e506877b0e1877a7f709fa9dc7895d5fbdcc93d4170dfb3d25e2839e9 AS buildtime

WORKDIR /build
COPY . .

RUN mvn clean package -DskipTests

#
# Docker RUNTIME
#
FROM amazoncorretto:17.0.9-alpine3.18@sha256:ed14b8c2f00dbb7b94446aa01d00583976ff0eda2577f5474035f3b4cf078dfd AS runtime

RUN apk --no-cache add shadow
RUN useradd --uid 10000 runner

VOLUME /tmp
WORKDIR /app

COPY --from=buildtime /build/target/*.jar /app/app.jar
# The agent is enabled at runtime via JAVA_TOOL_OPTIONS.
ADD https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.4.19/applicationinsights-agent-3.4.19.jar /app/applicationinsights-agent.jar

RUN chown -R runner:runner /app

USER 10000

ENTRYPOINT ["java","-jar","/app/app.jar"]
