FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY build ./build
COPY develocity-field.key ./build
COPY ge-gradle-org.key ./build

ENV METRICSPORT 8081
ENV DVSERVER develocity-field.gradle.com
ENV DVKEY develocity-field.key

ENTRYPOINT ./build/install/develocity-api-samples/bin/develocity-api-samples "builds" "--server-url=https://$DVSERVER" "--access-key-file=./build/$DVKEY"