#!/bin/bash

set -e  # Exit on error

echo "========================================="
echo "AuctionFlow Full Rebuild & Deploy"
echo "========================================="
echo ""

echo "=== Stopping Docker containers ==="
docker compose down

echo ""
echo "=== Building Frontend ==="
cd auctionflow-ui
npm run build
cd ..

echo ""
echo "=== Copying Frontend to Backend Static Resources ==="
rm -rf auction-api/src/main/resources/static/ui/*
cp -r auctionflow-ui/client/dist/* auction-api/src/main/resources/static/ui/
echo "✓ Frontend files copied successfully"

echo ""
echo "=== Building Backend Application ==="
./gradlew :auction-api:build -x test

echo ""
echo "=== Rebuilding Docker Images ==="
docker compose build --no-cache auction-api

echo ""
echo "=== Starting Docker Containers ==="
docker compose up -d

echo ""
echo "=== Waiting for services to start (40 seconds) ==="
for i in {1..40}; do
  echo -n "."
  sleep 1
done
echo ""

echo ""
echo "=== Checking Container Status ==="
docker compose ps

echo ""
echo "=== Checking auction-api logs ==="
docker compose logs auction-api --tail=50

echo ""
echo "========================================="
echo "Running Integration Tests"
echo "========================================="
echo ""

# Make test script executable and run it
chmod +x test-api-integration.sh
./test-api-integration.sh

echo ""
echo "========================================="
echo "Deployment Complete!"
echo "========================================="
echo ""
echo "✅ Frontend and Backend rebuilt successfully"
echo "✅ Docker containers are running"
echo "✅ Integration tests completed"
echo ""
echo "🌐 Access the application:"
echo "   - UI: http://localhost:8080/ui/"
echo "   - API: http://localhost:8080/api/v1"
echo ""
echo "📝 Next steps:"
echo "   1. Open http://localhost:8080/ui/ in your browser"
echo "   2. Try registering a new account"
echo "   3. Login and explore the auctions"
echo ""
