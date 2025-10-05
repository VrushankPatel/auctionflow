# AuctionFlow API Specification

## Overview

AuctionFlow is a production-grade backend service for running auctions reliably, fairly, and at low-latency. This API provides REST endpoints for creating auctions, placing bids, managing users, and handling payments.

## Authentication

All API endpoints require authentication using JWT/OAuth2 tokens. Include the token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

## Base URL

```
http://localhost:8080/api/v1
```

## Core Concepts

### Auction Types
- `ENGLISH_OPEN`: Standard ascending price auction
- `DUTCH`: Descending price auction
- `SEALED_BID`: Hidden bid amounts until close
- `RESERVE_PRICE`: Minimum price requirement
- `BUY_NOW`: Immediate purchase option

### Auction Status
- `PENDING`: Created but not yet active
- `ACTIVE`: Accepting bids
- `CLOSED`: Closed, winner determined
- `CANCELLED`: Terminated by seller/admin

### Bid Validation
- Minimum increment rules based on current highest bid
- Reserve price enforcement
- Anti-snipe extension policies
- Rate limiting per user/auction

## Authentication Endpoints

### POST /auth/login
User authentication with email/password.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "secure_password"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "displayName": "John Doe",
    "role": "BUYER"
  }
}
```

**Rate Limit:** 5 attempts/minute per IP

### POST /auth/register
New user registration.

**Request:**
```json
{
  "email": "newuser@example.com",
  "password": "secure_password",
  "displayName": "Jane Smith",
  "role": "SELLER"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "email": "newuser@example.com",
  "displayName": "Jane Smith",
  "kycStatus": "PENDING"
}
```

### POST /auth/refresh
Refresh JWT access token.

**Request:**
```json
{
  "refreshToken": "refresh_token_here"
}
```

## Auction Management

### POST /auctions
Create a new auction.

**Request:**
```json
{
  "item_id": "string",
  "type": "english_open|dutch|sealed_bid|reserve_price|buy_now",
  "start_ts": "2025-10-03T01:32:21Z",
  "end_ts": "2025-10-03T01:32:21Z",
  "reserve_price": 100.00,
  "buy_now_price": 500.00,
  "increment_strategy_id": "string",
  "extension_policy_id": "string"
}
```

**Response:**
```json
{
  "id": "string",
  "server_ts": "2025-10-03T01:32:21Z",
  "seq_no": 12345
}
```

### GET /auctions
List auctions with pagination.

**Query Parameters:**
- `category`: Filter by category
- `sellerId`: Filter by seller
- `page`: Page number (default 0)
- `size`: Page size (default 10)

**Response:**
```json
{
  "edges": [
    {
      "node": {
        "id": "string",
        "item": {...},
        "seller": {...},
        "type": "ENGLISH_OPEN",
        "status": "ACTIVE",
        "startTime": "2025-10-03T01:32:21Z",
        "endTime": "2025-10-03T01:32:21Z",
        "reservePrice": 100.00,
        "buyNowPrice": 500.00,
        "currentHighestBid": {...},
        "watchers": [...],
        "createdAt": "2025-10-03T01:32:21Z"
      },
      "cursor": "string"
    }
  ],
  "pageInfo": {
    "hasNextPage": true,
    "hasPreviousPage": false,
    "startCursor": "string",
    "endCursor": "string"
  },
  "totalCount": 100
}
```

### GET /auctions/{id}
Get auction details.

**Response:** Auction object

### PUT /auctions/{id}
Update auction.

**Request:**
```json
{
  "title": "string",
  "description": "string"
}
```

### DELETE /auctions/{id}
Close auction.

## Bidding

### POST /auctions/{auctionId}/bids
Place a bid.

**Request:**
```json
{
  "amount": 150.00,
  "idempotencyKey": "string"
}
```

**Response:**
```json
{
  "id": "string",
  "auction": {...},
  "bidder": {...},
  "amount": 150.00,
  "serverTimestamp": "2025-10-03T01:32:21Z",
  "sequenceNumber": 12345,
  "accepted": true
}
```

### GET /auctions/{auctionId}/bids
Get bids for auction.

**Query Parameters:**
- `page`: Page number
- `size`: Page size

**Response:** BidConnection

### GET /bids
Get user's bids.

**Query Parameters:**
- `page`: Page number
- `size`: Page size

## User Management

### GET /users
List users.

**Query Parameters:**
- `page`: Page number
- `size`: Page size

### GET /users/{id}
Get user details.

### PUT /users/{id}
Update user.

## Payments

### POST /payments
Process payment.

**Request:**
```json
{
  "auctionId": "string",
  "amount": 150.00,
  "paymentMethod": "CREDIT_CARD"
}
```

**Response:**
```json
{
  "id": "string",
  "status": "COMPLETED",
  "transactionId": "string"
}
```

## Reference Data

### GET /categories
Get auction categories.

### GET /currencies
Get supported currencies.

## WebSocket Subscriptions

### Bid Placed
Subscribe to bid events for an auction.

**Subscription:**
```graphql
subscription {
  bidPlaced(auctionId: "auction-id") {
    id
    amount
    bidder {
      id
      displayName
    }
  }
}
```

### Auction Closed
Subscribe to auction close events.

**Subscription:**
```graphql
subscription {
  auctionClosed(auctionId: "auction-id") {
    id
    winner {
      id
      displayName
    }
    finalPrice
  }
}
```

## GraphQL API

The API also provides a GraphQL endpoint at `/graphql` for flexible queries.

### Example Query
```graphql
query {
  auctions(category: "ART", page: 0, size: 10) {
    edges {
      node {
        id
        item {
          title
          description
        }
        currentHighestBid {
          amount
        }
      }
    }
    totalCount
  }
}
```

## Error Handling

All endpoints return standard HTTP status codes:
- `200`: Success
- `400`: Bad Request
- `401`: Unauthorized
- `403`: Forbidden
- `404`: Not Found
- `500`: Internal Server Error

Error responses include:
```json
{
  "error": "Error message",
  "code": "ERROR_CODE"
}
```

## Rate Limiting

- Authentication: 5 requests/minute per IP
- Bidding: 10 requests/minute per user
- General API: 100 requests/minute per user

## Data Types

### Auction
```json
{
  "id": "string",
  "item": "Item",
  "seller": "User",
  "type": "AuctionType",
  "status": "AuctionStatus",
  "startTime": "DateTime",
  "endTime": "DateTime",
  "reservePrice": "Money",
  "buyNowPrice": "Money",
  "currentHighestBid": "Bid",
  "bids": "BidConnection",
  "watchers": ["User"],
  "createdAt": "DateTime"
}
```

### Bid
```json
{
  "id": "string",
  "auction": "Auction",
  "bidder": "User",
  "amount": "Money",
  "serverTimestamp": "DateTime",
  "sequenceNumber": "Int",
  "accepted": "Boolean"
}
```

### User
```json
{
  "id": "string",
  "email": "string",
  "displayName": "string",
  "role": "UserRole",
  "kycStatus": "KycStatus",
  "auctions": "AuctionConnection",
  "bids": "BidConnection",
  "createdAt": "DateTime"
}
```

### Item
```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "category": "string",
  "images": ["string"],
  "metadata": "string"
}
```

### Money
A monetary value represented as a string (e.g., "150.00")

### DateTime
ISO 8601 formatted date-time string (e.g., "2025-10-03T01:32:21Z")

## Enums

### AuctionType
- ENGLISH_OPEN
- DUTCH
- SEALED_BID
- RESERVE_PRICE
- BUY_NOW

### AuctionStatus
- PENDING
- ACTIVE
- CLOSED
- CANCELLED

### UserRole
- BUYER
- SELLER
- ADMIN

### KycStatus
- PENDING
- VERIFIED
- REJECTED

## Pagination

All list endpoints support cursor-based pagination:

```json
{
  "edges": [
    {
      "node": {...},
      "cursor": "string"
    }
  ],
  "pageInfo": {
    "hasNextPage": "boolean",
    "hasPreviousPage": "boolean",
    "startCursor": "string",
    "endCursor": "string"
  },
  "totalCount": "int"
}
```

Use `cursor` for subsequent requests to get the next page.