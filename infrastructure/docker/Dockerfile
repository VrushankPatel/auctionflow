# Final stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Copy application files
COPY auction-api/build/libs/auction-api-1.0.0-SNAPSHOT.jar app.jar
COPY auction-api/src/main/resources/db/migration /app/db/migration
COPY auction-api/src/main/resources/static /app/static

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.redis.host=redis-1", "-Dspring.redis.port=6379", "-jar", "app.jar"]