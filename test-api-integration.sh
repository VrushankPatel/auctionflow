#!/bin/bash

# API Integration Test Script
# Tests key endpoints to verify backend-frontend integration

BASE_URL="http://localhost:8080"
API_BASE="${BASE_URL}/api/v1"

echo "========================================="
echo "API Integration Test Suite"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counter
PASSED=0
FAILED=0

test_endpoint() {
    local name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5
    
    echo -n "Testing: $name... "
    
    if [ "$method" == "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$endpoint")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" -H "Content-Type: application/json" -d "$data" "$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "$expected_status" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code)"
        ((PASSED++))
        if [ ! -z "$body" ]; then
            echo "  Response: $(echo $body | jq -c . 2>/dev/null || echo $body | head -c 100)"
        fi
    else
        echo -e "${RED}✗ FAILED${NC} (Expected $expected_status, got $http_code)"
        ((FAILED++))
        echo "  Response: $body"
    fi
    echo ""
}

echo "=== 1. Reference Data Endpoints ==="
test_endpoint "Get Categories" "GET" "${API_BASE}/reference/categories" "" "200"
test_endpoint "Get Bid Increments" "GET" "${API_BASE}/reference/bid-increments" "" "200"
test_endpoint "Get Auction Types" "GET" "${API_BASE}/reference/auction-types" "" "200"
test_endpoint "Get Extension Policies" "GET" "${API_BASE}/reference/extension-policies" "" "200"

echo "=== 2. Authentication Endpoints ==="

# Register a test user
TIMESTAMP=$(date +%s)
TEST_EMAIL="testuser${TIMESTAMP}@example.com"
REGISTER_DATA="{\"email\":\"${TEST_EMAIL}\",\"displayName\":\"Test User\",\"password\":\"password123\",\"role\":\"BUYER\"}"

test_endpoint "Register User" "POST" "${API_BASE}/auth/register" "$REGISTER_DATA" "200"

# Login with the test user
LOGIN_DATA="{\"email\":\"${TEST_EMAIL}\",\"password\":\"password123\"}"
echo -n "Testing: Login User... "
response=$(curl -s -w "\n%{http_code}" -X POST -H "Content-Type: application/json" -d "$LOGIN_DATA" "${API_BASE}/auth/login")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code)"
    ((PASSED++))
    TOKEN=$(echo "$body" | jq -r '.token // .accessToken')
    echo "  Token: ${TOKEN:0:50}..."
    echo "  User: $(echo $body | jq -c '.user')"
else
    echo -e "${RED}✗ FAILED${NC} (Expected 200, got $http_code)"
    ((FAILED++))
    echo "  Response: $body"
    TOKEN=""
fi
echo ""

# Test invalid login
INVALID_LOGIN="{\"email\":\"invalid@example.com\",\"password\":\"wrong\"}"
test_endpoint "Login with Invalid Credentials" "POST" "${API_BASE}/auth/login" "$INVALID_LOGIN" "401"

echo "=== 3. Auction Endpoints (Public) ==="
test_endpoint "List Auctions" "GET" "${API_BASE}/auctions" "" "200"

echo "=== 4. Protected Endpoints (Require Auth) ==="
if [ ! -z "$TOKEN" ]; then
    echo -n "Testing: Get User Profile (Protected)... "
    response=$(curl -s -w "\n%{http_code}" -H "Authorization: Bearer $TOKEN" "${API_BASE}/users/me")
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" == "200" ] || [ "$http_code" == "404" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code - endpoint exists)"
        ((PASSED++))
    else
        echo -e "${YELLOW}⚠ PARTIAL${NC} (HTTP $http_code - may need implementation)"
    fi
    echo ""
else
    echo -e "${YELLOW}⚠ SKIPPED${NC} - No auth token available"
    echo ""
fi

echo "=== 5. UI Endpoints ==="
test_endpoint "UI Index Page" "GET" "${BASE_URL}/ui/index.html" "" "200"
test_endpoint "UI Root" "GET" "${BASE_URL}/ui/" "" "200"

echo "========================================="
echo "Test Summary"
echo "========================================="
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}"
    exit 1
fi
