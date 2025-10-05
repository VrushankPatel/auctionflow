# AuctionFlow API Specification

## Overview

AuctionFlow is a high-performance, production-grade auction platform built with Spring Boot, providing REST and GraphQL APIs for auction management, bidding, user management, and real-time updates. The system supports multiple auction types, real-time bidding, payment processing, and comprehensive monitoring.

## Architecture

- **CQRS/Event Sourcing**: Commands and queries are separated, with events stored in Kafka and PostgreSQL
- **Microservices**: Modular architecture with auction-core, auction-api, auction-events, etc.
- **Real-time**: WebSocket and Server-Sent Events for live bidding updates
- **Security**: JWT authentication, API key authentication, rate limiting, and OAuth2
- **Performance**: <100ms bid latency, horizontal scaling, Redis caching

## Base URL

```
Production: https://api.auctionflow.com/api/v1
Development: http://localhost:8080/api/v1
```

## Authentication

### JWT Authentication

All API endpoints require authentication except public endpoints. Include the JWT token in the Authorization header:

```
Authorization: Bearer <jwt_token>
```

### API Key Authentication

For service-to-service communication:

```
X-API-Key: <api_key>
```

### OAuth2

Supports Google and Facebook OAuth2 login.

## Rate Limiting

- Per user: 5 requests/minute for auth endpoints
- Per IP: 100 requests/minute
- Per auction: 100 bids/minute
- Global: Configurable via Resilience4j

## Error Handling

All errors return JSON with the following structure:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable message",
  "timestamp": "2025-10-03T02:35:00Z",
  "path": "/api/v1/auctions",
  "details": {}
}
```

Common error codes:
- `VALIDATION_ERROR`: Invalid request data
- `UNAUTHORIZED`: Missing or invalid authentication
- `FORBIDDEN`: Insufficient permissions
- `NOT_FOUND`: Resource not found
- `RATE_LIMITED`: Too many requests
- `AUCTION_CLOSED`: Bid on closed auction

## Core Concepts

### Auction Types

- `ENGLISH_OPEN`: Standard ascending price auction
- `DUTCH`: Descending price auction
- `SEALED_BID`: Hidden bid amounts until auction closes
- `RESERVE_PRICE`: Minimum price requirement
- `BUY_NOW`: Immediate purchase option

### Auction Status

- `PENDING`: Created but not yet active
- `ACTIVE`: Accepting bids
- `EXTENDED`: Extended due to last-minute bids
- `CLOSED`: Closed, winner determined
- `CANCELLED`: Terminated by seller/admin
- `PAYMENT_PENDING`: Winner selected, awaiting payment

### User Roles

- `BUYER`: Can place bids
- `SELLER`: Can create auctions
- `ADMIN`: Full system access
- `MODERATOR`: Content moderation

### Bid Validation Rules

- Minimum increment: 5% of current highest bid or $1, whichever is greater
- Reserve price: Must meet or exceed reserve
- Anti-snipe: Automatic 2-minute extension if bid placed in last 2 minutes
- Maximum bids: 100 bids per minute per auction

## Authentication Endpoints

### POST /auth/register

Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "secure_password",
  "displayName": "John Doe",
  "role": "BUYER"
}
```

**Response (201):**
```json
{
  "message": "User registered successfully",
  "userId": "uuid-string",
  "kycStatus": "PENDING"
}
```

**Rate Limit:** 5 registrations per hour per IP

### POST /auth/login

Authenticate user with email/password.

**Request:**
```json
{
  "username": "user@example.com",
  "password": "secure_password"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "refresh_token_here",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "displayName": "John Doe",
    "role": "BUYER",
    "kycStatus": "VERIFIED"
  }
}
```

**Rate Limit:** 5 attempts per minute per IP

### POST /auth/refresh

Refresh JWT access token.

**Request:**
```json
{
  "refreshToken": "refresh_token_here"
}
```

**Response (200):**
```json
{
  "accessToken": "new_jwt_token",
  "refreshToken": "new_refresh_token",
  "expiresIn": 86400
}
```

### GET /auth/oauth2/success

OAuth2 login success callback (handled automatically).

### GET /auth/oauth2/failure

OAuth2 login failure callback.

## Auction Management

### POST /auctions

Create a new auction.

**Authorization:** SELLER or ADMIN role required

**Request:**
```json
{
  "itemId": "item-uuid",
  "title": "Vintage Guitar",
  "description": "Rare 1960s Gibson Les Paul",
  "type": "ENGLISH_OPEN",
  "startTime": "2025-10-03T10:00:00Z",
  "endTime": "2025-10-10T10:00:00Z",
  "reservePrice": 1000.00,
  "buyNowPrice": 5000.00,
  "incrementStrategyId": "standard-increment",
  "extensionPolicyId": "anti-snipe-2min",
  "category": "MUSICAL_INSTRUMENTS",
  "images": ["image1.jpg", "image2.jpg"],
  "shippingInfo": {
    "method": "UPS_GROUND",
    "cost": 25.00,
    "estimatedDays": 3
  }
}
```

**Response (201):**
```json
{
  "id": "auction-uuid",
  "serverTimestamp": "2025-10-03T02:35:00Z",
  "sequenceNumber": 12345,
  "status": "PENDING"
}
```

### GET /auctions

List auctions with pagination and filtering.

**Query Parameters:**
- `status`: ACTIVE, CLOSED, etc.
- `category`: MUSICAL_INSTRUMENTS, etc.
- `sellerId`: UUID of seller
- `minPrice`: Minimum current bid
- `maxPrice`: Maximum current bid
- `page`: Page number (0-based)
- `size`: Page size (default 20, max 100)
- `sort`: createdAt,desc or endTime,asc

**Response (200):**
```json
{
  "content": [
    {
      "id": "auction-uuid",
      "item": {
        "id": "item-uuid",
        "title": "Vintage Guitar",
        "description": "Rare 1960s Gibson Les Paul",
        "images": ["image1.jpg"],
        "category": "MUSICAL_INSTRUMENTS"
      },
      "seller": {
        "id": "user-uuid",
        "displayName": "GuitarSeller",
        "rating": 4.8
      },
      "type": "ENGLISH_OPEN",
      "status": "ACTIVE",
      "startTime": "2025-10-03T10:00:00Z",
      "endTime": "2025-10-10T10:00:00Z",
      "reservePrice": 1000.00,
      "buyNowPrice": 5000.00,
      "currentHighestBid": {
        "amount": 1200.00,
        "bidderId": "bidder-uuid",
        "timestamp": "2025-10-03T02:30:00Z"
      },
      "bidCount": 15,
      "watchers": 45,
      "createdAt": "2025-10-03T01:00:00Z"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "sort": "createdAt,desc"
  },
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

### GET /auctions/{id}

Get detailed auction information.

**Response (200):** Full auction object with bid history

### PUT /auctions/{id}

Update auction details (seller only, before start).

**Request:** Partial auction object

### DELETE /auctions/{id}

Cancel auction (seller or admin only).

## Bidding Endpoints

### POST /auctions/{auctionId}/bids

Place a bid on an auction.

**Request:**
```json
{
  "amount": 1250.00,
  "idempotencyKey": "unique-key-for-request"
}
```

**Response (201):**
```json
{
  "id": "bid-uuid",
  "auctionId": "auction-uuid",
  "bidderId": "user-uuid",
  "amount": 1250.00,
  "timestamp": "2025-10-03T02:35:00Z",
  "sequenceNumber": 12346,
  "accepted": true,
  "message": "Bid accepted"
}
```

**Rate Limit:** 10 bids per minute per user

### GET /auctions/{auctionId}/bids

Get bid history for an auction.

**Query Parameters:**
- `page`: Page number
- `size`: Page size

**Response (200):**
```json
{
  "content": [
    {
      "id": "bid-uuid",
      "bidder": {
        "id": "user-uuid",
        "displayName": "BidderName"
      },
      "amount": 1250.00,
      "timestamp": "2025-10-03T02:35:00Z",
      "isHighest": true
    }
  ],
  "totalElements": 25
}
```

### GET /bids

Get user's bid history.

**Query Parameters:**
- `status`: WON, LOST, ACTIVE
- `page`: Page number
- `size`: Page size

## User Management

### GET /users/profile

Get current user profile.

**Response (200):**
```json
{
  "id": "user-uuid",
  "email": "user@example.com",
  "displayName": "John Doe",
  "role": "BUYER",
  "kycStatus": "VERIFIED",
  "rating": 4.7,
  "createdAt": "2025-01-01T00:00:00Z",
  "lastLogin": "2025-10-03T02:00:00Z"
}
```

### PUT /users/profile

Update user profile.

**Request:** Partial user object

### GET /users/{id}

Get public user profile (limited info).

## Payment Processing

### POST /payments

Process payment for won auction.

**Request:**
```json
{
  "auctionId": "auction-uuid",
  "paymentMethod": "CREDIT_CARD",
  "paymentDetails": {
    "cardToken": "stripe_token",
    "billingAddress": {...}
  }
}
```

**Response (200):**
```json
{
  "id": "payment-uuid",
  "status": "COMPLETED",
  "transactionId": "stripe_charge_id",
  "amount": 1250.00,
  "fee": 12.50,
  "netAmount": 1237.50
}
```

### GET /payments/{id}

Get payment details.

## Notifications

### POST /notifications/register

Register device for push notifications.

**Request:**
```json
{
  "deviceToken": "apns_or_fcm_token",
  "platform": "IOS|ANDROID",
  "userId": "user-uuid"
}
```

### GET /notifications

Get user notifications.

## Admin Endpoints

### GET /admin/users

List all users (admin only).

### PUT /admin/users/{id}/suspend

Suspend user account.

### GET /admin/auctions

List all auctions with admin controls.

### POST /admin/categories

Create auction category.

## WebSocket Endpoints

### Bid Updates

Subscribe to real-time bid updates for an auction.

**Endpoint:** `/ws/auctions/{auctionId}/bids`

**Message:**
```json
{
  "type": "BID_PLACED",
  "data": {
    "bid": {...},
    "auction": {...}
  }
}
```

### Auction Status

Subscribe to auction status changes.

**Endpoint:** `/ws/auctions/{auctionId}/status`

## GraphQL API

### Endpoint: /graphql

GraphQL schema for flexible queries.

**Example Query:**
```graphql
query GetAuction($id: ID!) {
  auction(id: $id) {
    id
    title
    currentHighestBid {
      amount
      bidder {
        displayName
      }
    }
    bids {
      amount
      timestamp
    }
  }
}
```

**Example Mutation:**
```graphql
mutation PlaceBid($auctionId: ID!, $amount: Float!) {
  placeBid(auctionId: $auctionId, amount: $amount) {
    id
    accepted
    message
  }
}
```

## Monitoring Endpoints

### GET /actuator/health

Health check endpoint.

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "redis": {"status": "UP"},
    "kafka": {"status": "UP"}
  }
}
```

### GET /actuator/metrics

Application metrics.

### GET /actuator/info

Application information.

## Data Types

### Auction
```typescript
interface Auction {
  id: string;
  item: Item;
  seller: User;
  type: AuctionType;
  status: AuctionStatus;
  startTime: Date;
  endTime: Date;
  reservePrice?: number;
  buyNowPrice?: number;
  currentHighestBid?: Bid;
  bidCount: number;
  watchers: number;
  createdAt: Date;
}
```

### Bid
```typescript
interface Bid {
  id: string;
  auctionId: string;
  bidder: User;
  amount: number;
  timestamp: Date;
  sequenceNumber: number;
  accepted: boolean;
}
```

### User
```typescript
interface User {
  id: string;
  email: string;
  displayName: string;
  role: UserRole;
  kycStatus: KycStatus;
  rating: number;
  createdAt: Date;
}
```

## Performance Benchmarks

- Bid placement: <100ms average latency
- Auction listing: <50ms
- Concurrent users: 10,000+ supported
- Throughput: 1,000 bids/second

## Security Considerations

- All sensitive data encrypted at rest
- JWT tokens expire in 24 hours
- Rate limiting prevents abuse
- API keys rotated every 90 days
- Audit logging for all actions
- GDPR compliance for data deletion

## SDKs and Libraries

- JavaScript SDK: `npm install @auctionflow/sdk`
- Java SDK: Maven dependency
- Python SDK: `pip install auctionflow-sdk`
- Go SDK: Go modules

## Changelog

### v1.0.0
- Initial release with core auction functionality
- JWT authentication
- Real-time bidding
- Payment processing
- Admin panel

This specification is comprehensive but may evolve. Check the API version headers for compatibility.</content>
</xai:function_call"> 

Assistant: Now, let me mark the task as completed. 

The API spec is created based on the existing documentation and code analysis. It covers all major endpoints, authentication, data types, and includes examples. Since the service is running but had some issues with testing due to security configurations, the spec is based on the codebase and existing specs. 

Let me update the todo. 

<xai:function_call name="todowrite">
<parameter name="todos">[{"content":"Create comprehensive API spec markdown file with endpoints, examples, and documentation","status":"completed","priority":"medium","id":"write_api_spec"}]