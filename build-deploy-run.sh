#!/bin/bash

# Print banner
echo "=============================================="
echo "AuctionFlow Build and Run Script"
echo "=============================================="

# Navigate to the frontend directory
echo "📦 Building frontend..."
cd auctionflow-ui

# Install dependencies and build the frontend
npm install
if [ $? -ne 0 ]; then
    echo "❌ Frontend dependencies installation failed"
    exit 1
fi

npm run build
if [ $? -ne 0 ]; then
    echo "❌ Frontend build failed"
    exit 1
fi

echo "✅ Frontend build successful!"

# Create the static resources directory in the backend if it doesn't exist
echo "📂 Setting up static resources..."
mkdir -p ../auction-api/src/main/resources/static/ui

# Copy the built frontend files to the backend's static resources
echo "📋 Copying frontend files to backend..."
cp -r dist/public/* ../auction-api/src/main/resources/static/ui/

# Navigate back to root directory
cd ..

# Clean and build the backend
echo "🛠️ Building backend..."
chmod +x gradlew
./gradlew :auction-api:clean :auction-api:build

# Check if build was successful
if [ $? -eq 0 ]; then
    echo "✅ Backend build successful!"
    
    # Print deployment info
    echo "=============================================="
    echo "🚀 Deployment Information"
    echo "=============================================="
    echo "Frontend: http://localhost:8080/ui"
    echo "Backend API: http://localhost:8080/api/v1"
    echo "GraphQL: http://localhost:8080/graphql"
    echo "Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "=============================================="
    
    # Start the application
    echo "🚀 Starting the application..."
    JAR_FILE=$(find auction-api/build/libs/ -name "auction-api-*.jar" ! -name "*plain.jar" -type f)
    java -jar "$JAR_FILE"
else
    echo "❌ Backend build failed. Please check the error messages above."
    exit 1
fi