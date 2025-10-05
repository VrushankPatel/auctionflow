#!/bin/bash

echo "========================================="
echo "Quick Update - Backend Only"
echo "========================================="
echo ""

echo "=== Stopping auction-api container ==="
docker compose stop auction-api

echo ""
echo "=== Building Backend Application ==="
./gradlew :auction-api:build -x test

echo ""
echo "=== Rebuilding Docker Image ==="
docker compose build --no-cache auction-api

echo ""
echo "=== Starting auction-api container ==="
docker compose up -d auction-api

echo ""
echo "=== Waiting for service to start (20 seconds) ==="
for i in {1..20}; do
  echo -n "."
  sleep 1
done
echo ""

echo ""
echo "=== Checking auction-api logs ==="
docker compose logs auction-api --tail=30

echo ""
echo "=== Testing Key Endpoints ==="
echo ""
echo "1. Testing /api/v1/auctions:"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/api/v1/auctions

echo ""
echo "2. Testing WebSocket endpoint (should return 400 for HTTP):"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/ws

echo ""
echo "========================================="
echo "Update Complete!"
echo "========================================="
echo ""
echo "✅ Backend updated and restarted"
echo ""
echo "🌐 Open http://localhost:8080/ui/ and check:"
echo "   - Auctions should load (no 403 error)"
echo "   - WebSocket should connect (no reconnecting alert)"
echo ""
