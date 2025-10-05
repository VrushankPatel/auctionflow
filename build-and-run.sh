#!/bin/bash

# Print banner
echo "====================================="
echo "AuctionFlow Build and Run Script"
echo "====================================="

# Set environment variables
export SPRING_PROFILES_ACTIVE=dev
export POSTGRES_DB=auctionflow
export POSTGRES_USER=auction_user
export POSTGRES_PASSWORD=secure_password

# Build frontend
echo "Building frontend..."
cd auctionflow-ui
npm install
npm run build
cd ..

# Create static directory if it doesn't exist
echo "Setting up static resources..."
mkdir -p auction-api/src/main/resources/static/ui

# Copy frontend build to backend static directory
echo "Copying frontend build to backend..."
cp -r auctionflow-ui/dist/public/* auction-api/src/main/resources/static/ui/

# Build backend
echo "Building backend..."
chmod +x gradlew
./gradlew clean build

# Start PostgreSQL if not running
echo "Ensuring PostgreSQL is running..."
if ! docker ps | grep -q postgres-primary; then
    docker-compose up -d postgres-primary
    echo "Waiting for PostgreSQL to start..."
    sleep 10
fi

# Start Redis if not running
echo "Ensuring Redis is running..."
if ! docker ps | grep -q redis-1; then
    docker-compose up -d redis-1
    echo "Waiting for Redis to start..."
    sleep 5
fi

# Print application info
echo "====================================="
echo "Application Info:"
echo "Frontend: http://localhost:8080/ui"
echo "Backend API: http://localhost:8080/api/v1"
echo "GraphQL UI: http://localhost:8080/graphiql"
echo "Swagger UI: http://localhost:8080/swagger-ui.html"
echo "====================================="

# Run the application in background
echo "Starting the application in background..."
# Use the executable JAR (not the plain one)
JAR_FILE=$(find auction-api/build/libs/ -name "auction-api-*.jar" ! -name "*plain.jar")

# Run with optimized JVM settings
nohup java -Dspring.profiles.active=dev \
    -Xms512m -Xmx1g \
    -XX:+UseG1GC \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=./heapdump.hprof \
    -XX:+PrintGCDetails \
    -XX:+PrintGCDateStamps \
    -Xloggc:gc.log \
    -jar $JAR_FILE > app.log 2>&1 &

# Store the PID
APP_PID=$!
echo "Application started with PID: $APP_PID"

# Wait for 1 minute
echo "Waiting for 1 minute to check application status..."
sleep 60

# Check if process is still running
if ps -p $APP_PID > /dev/null; then
    echo "Application is still running. Showing last 50 lines of logs:"
    echo "====================================="
    tail -n 50 app.log
    echo "====================================="
    echo "To continue following logs, use: tail -f app.log"
    echo "To check garbage collection logs, use: tail -f gc.log"
    echo "To stop the application, use: kill $APP_PID"
else
    echo "Application failed to start. Showing logs:"
    cat app.log
    exit 1
fi
