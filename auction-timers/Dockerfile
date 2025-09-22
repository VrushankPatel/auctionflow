# Multi-stage build for auction-timers
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy gradle files for dependency caching
COPY ../build.gradle ../settings.gradle ./
COPY build.gradle auction-timers/
COPY ../auction-core/build.gradle auction-core/
COPY ../auction-common/build.gradle auction-common/
COPY ../auction-events/build.gradle auction-events/

# Download dependencies
RUN gradle dependencies --no-daemon

# Copy source code
COPY .. .

# Build the jar
RUN gradle :auction-timers:bootJar --no-daemon -x test

FROM openjdk:17-jre-slim

WORKDIR /app

# Install Flyway CLI for migrations
RUN apt-get update && apt-get install -y wget unzip && \
    wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/10.0.1/flyway-commandline-10.0.1-linux-x64.tar.gz | tar xvz && \
    ln -s `pwd`/flyway-10.0.1/flyway /usr/local/bin/flyway && \
    apt-get remove -y wget unzip && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

 COPY --from=build /app/auction-timers/build/libs/*.jar app.jar
 COPY --from=build /app/auction-timers/src/main/resources/db/migration /app/db/migration

 # Download async-profiler for advanced profiling
 RUN wget -qO- https://github.com/async-profiler/async-profiler/releases/download/v2.9/async-profiler-2.9-linux-x64.tar.gz | tar xvz && mv async-profiler-2.9 /app/async-profiler

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]