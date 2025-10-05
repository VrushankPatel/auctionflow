# AuctionFlow API Specification

## Overview

AuctionFlow is a high-performance auction platform built with Spring Boot, providing REST APIs for auction management, bidding, user operations, and real-time features. This specification covers all available endpoints with detailed request/response formats, authentication requirements, and usage examples.

**Base URL:** `https://api.auctionflow.com/v1`  
**Authentication:** JWT Bearer tokens required for all endpoints except public reference data  
**Content-Type:** `application/json`  
**Rate Limits:** Vary by endpoint (see individual endpoint documentation)

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Authentication Endpoints](#authentication-endpoints)
3. [Auction Management](#auction-management)
4. [Bidding Operations](#bidding-operations)
5. [Item Management](#item-management)
6. [User Management](#user-management)
7. [Watchlist & Notifications](#watchlist--notifications)
8. [Automated Bidding](#automated-bidding)
9. [Reference Data](#reference-data)
10. [Administrative Operations](#administrative-operations)
11. [Payment Integration](#payment-integration)
12. [Real-time Features](#real-time-features)
13. [Error Handling](#error-handling)
14. [Rate Limiting](#rate-limiting)
15. [Data Types](#data-types)
16. [Security Considerations](#security-considerations)
17. [Performance Characteristics](#performance-characteristics)
18. [Versioning](#versioning)
19. [Testing](#testing)
20. [Support](#support)

## Core Concepts

### Auction Types
- `ENGLISH_OPEN`: Standard ascending price auction
- `DUTCH`: Descending price auction
- `SEALED_BID`: Hidden bid amounts until close
- `RESERVE_PRICE`: Minimum price requirement
- `BUY_NOW`: Immediate purchase option

### Auction Status
- `PENDING`: Created but not yet active
- `OPEN`: Accepting bids
- `ENDED`: Closed, winner determined
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
  "itemId": "uuid",
  "type": "ENGLISH_OPEN",
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T12:00:00Z",
  "reservePrice": 100.00,
  "buyNowPrice": 500.00,
  "incrementStrategyId": "uuid",
  "extensionPolicyId": "uuid"
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "status": "PENDING",
  "serverTs": "2024-01-01T09:00:00Z",
  "seqNo": 12345
}
```

**Validation:**
- End time must be in future
- Reserve price must be positive
- Buy-now price must exceed reserve

### GET /auctions
List auctions with filtering and pagination.

**Query Parameters:**
- `status`: OPEN, CLOSED, CANCELLED
- `category`: Item category filter
- `sellerId`: Filter by seller
- `query`: Full-text search
- `page`: Page number (0-based)
- `size`: Page size (max 100)

**Response (200):**
```json
{
  "auctions": [
    {
      "id": "uuid",
      "title": "Vintage Watch",
      "currentHighestBid": 150.00,
      "bidCount": 5,
      "endTime": "2024-01-01T12:00:00Z",
      "status": "OPEN"
    }
  ],
  "total": 150,
  "page": 0,
  "hasMore": true
}
```

### GET /auctions/{id}
Get detailed auction information.

**Response (200):**
```json
{
  "id": "uuid",
  "item": {
    "title": "Vintage Watch",
    "description": "Rare collectible",
    "category": "watches",
    "images": ["url1.jpg"]
  },
  "seller": {
    "id": "uuid",
    "displayName": "WatchSeller"
  },
  "currentHighestBid": 150.00,
  "bidCount": 5,
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-01T12:00:00Z",
  "reservePrice": 100.00,
  "buyNowPrice": 500.00,
  "status": "OPEN"
}
```

### PATCH /auctions/{id}
Update auction metadata before first bid.

**Request:**
```json
{
  "title": "Updated Title",
  "description": "Updated description"
}
```

**Response (200):**
```json
{
  "id": "uuid",
  "updated": true
}
```

**Restrictions:** Only allowed before first bid is placed.

## Bidding Operations

### POST /auctions/{id}/bids
Place a bid on an auction.

**Headers:**
```
Idempotency-Key: optional-unique-key
```

**Request:**
```json
{
  "amount": 160.00
}
```

**Response (200):**
```json
{
  "accepted": true,
  "serverTs": "2024-01-01T11:30:00Z",
  "seqNo": 12346,
  "newHighest": true
}
```

**Error Responses:**
- `400`: Bid too low, auction ended, etc.
- `429`: Rate limit exceeded

### POST /auctions/{id}/bulk-bids
Place multiple bids in a single request.

**Request:**
```json
[
  {
    "amount": 160.00,
    "idempotencyKey": "key1"
  },
  {
    "amount": 170.00,
    "idempotencyKey": "key2"
  }
]
```

**Response (200):**
```json
[
  {
    "accepted": true,
    "serverTs": "2024-01-01T11:30:00Z",
    "seqNo": 12346
  },
  {
    "accepted": true,
    "serverTs": "2024-01-01T11:30:01Z",
    "seqNo": 12347
  }
]
```

### GET /auctions/{id}/bids
Get bid history for an auction.

**Query Parameters:**
- `page`: Page number
- `size`: Page size (max 50)

**Response (200):**
```json
{
  "bids": [
    {
      "id": "uuid",
      "bidderId": "uuid",
      "amount": 160.00,
      "serverTs": "2024-01-01T11:30:00Z",
      "seqNo": 12346,
      "accepted": true
    }
  ],
  "total": 25,
  "page": 0
}
```

### POST /auctions/{id}/buy-now
Execute immediate purchase.

**Response (200):**
```json
{
  "purchased": true,
  "finalPrice": 500.00,
  "serverTs": "2024-01-01T11:45:00Z",
  "seqNo": 12348
}
```

## Item Management

### POST /items
Create a new item.

**Request:**
```json
{
  "title": "Vintage Camera",
  "description": "Professional film camera",
  "categoryId": "electronics",
  "brand": "Nikon",
  "serialNumber": "123456",
  "images": ["image1.jpg", "image2.jpg"],
  "metadata": {
    "condition": "excellent",
    "year": "1990"
  }
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "sellerId": "uuid",
  "title": "Vintage Camera",
  "createdAt": "2024-01-01T09:00:00Z"
}
```

### GET /items
List items with optional seller filter.

**Query Parameters:**
- `sellerId`: Filter by seller
- `category`: Filter by category

**Response (200):**
```json
{
  "items": [
    {
      "id": "uuid",
      "title": "Vintage Camera",
      "category": "electronics",
      "images": ["image1.jpg"],
      "createdAt": "2024-01-01T09:00:00Z"
    }
  ],
  "total": 50
}
```

### GET /items/{id}
Get item details.

### PUT /items/{id}
Update item (seller only).

### DELETE /items/{id}
Delete item (seller only).

## User Management

### POST /users
Create a new user account.

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
  "id": "uuid",
  "email": "user@example.com",
  "displayName": "John Doe",
  "kycStatus": "PENDING"
}
```

### GET /users/{id}/bids
Get user's bid history.

**Response (200):**
```json
{
  "bids": [
    {
      "auctionId": "uuid",
      "auctionTitle": "Vintage Watch",
      "amount": 160.00,
      "status": "WINNING",
      "timestamp": "2024-01-01T11:30:00Z"
    }
  ],
  "total": 15
}
```

## Watchlist & Notifications

### POST /auctions/{id}/watch
Add auction to user's watchlist.

**Response (200):**
```json
{
  "watched": true
}
```

### DELETE /auctions/{id}/watch
Remove auction from watchlist.

**Response (200):**
```json
{
  "unwatched": true
}
```

## Automated Bidding

### POST /automated-bidding/strategies
Create automated bidding strategy.

**Request:**
```json
{
  "auctionId": "uuid",
  "strategyType": "MAX_BID",
  "maxAmount": 500.00,
  "incrementAmount": 10.00
}
```

**Response (201):**
```json
{
  "id": "uuid",
  "auctionId": "uuid",
  "status": "ACTIVE",
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### GET /automated-bidding/strategies
List user's automated bidding strategies.

**Response (200):**
```json
{
  "strategies": [
    {
      "id": "uuid",
      "auctionId": "uuid",
      "strategyType": "MAX_BID",
      "maxAmount": 500.00,
      "status": "ACTIVE"
    }
  ]
}
```

### DELETE /automated-bidding/strategies/{id}
Deactivate automated bidding strategy.

## Reference Data

### GET /reference/categories
Get item categories.

**Response (200):**
```json
{
  "categories": [
    {
      "id": "electronics",
      "name": "Electronics",
      "parentId": null
    },
    {
      "id": "watches",
      "name": "Watches",
      "parentId": "electronics"
    }
  ]
}
```

### GET /reference/bid-increments
Get bid increment strategies.

**Response (200):**
```json
{
  "increments": [
    {
      "minAmount": 0.00,
      "maxAmount": 100.00,
      "increment": 5.00
    },
    {
      "minAmount": 100.00,
      "maxAmount": 1000.00,
      "increment": 10.00
    }
  ]
}
```

### GET /reference/auction-types
Get available auction types.

**Response (200):**
```json
{
  "types": [
    {
      "id": "ENGLISH_OPEN",
      "name": "English Open Auction",
      "description": "Standard ascending price auction"
    }
  ]
}
```

### GET /reference/extension-policies
Get auction extension policies.

**Response (200):**
```json
{
  "policies": [
    {
      "id": "uuid",
      "type": "FIXED_WINDOW",
      "value": 300,
      "description": "Extend by 5 minutes if bid in last 5 minutes"
    }
  ]
}
```

## Administrative Operations

### POST /auctions/{id}/close
Admin force close auction.

**Response (200):**
```json
{
  "closed": true,
  "winnerId": "uuid",
  "finalPrice": 200.00
}
```

## Payment Integration

### POST /payments/webhook
Handle payment provider webhooks.

**Headers:**
```
X-Webhook-Signature: signature_from_provider
```

**Response (200):**
```json
{
  "processed": true
}
```

## Real-time Features

### WebSocket Connection
**Endpoint:** `/ws/notifications`

**Authentication:** JWT token in connection

**Events:**
- `bid_placed`: New bid on watched auction
- `auction_extended`: Auction end time extended
- `auction_closed`: Auction ended with winner
- `auction_cancelled`: Auction terminated

**Message Format:**
```json
{
  "event": "bid_placed",
  "auctionId": "uuid",
  "data": {
    "amount": 160.00,
    "bidderId": "uuid",
    "serverTs": "2024-01-01T11:30:00Z"
  },
  "timestamp": "2024-01-01T11:30:00Z"
}
```

### Server-Sent Events (SSE)
**Endpoint:** `/api/v1/notifications/sse`

**Fallback for WebSocket-unavailable clients**

## Error Handling

All endpoints return standard HTTP status codes with detailed error responses:

### Common Error Response Format
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {
    "field": "validation_error_details"
  },
  "timestamp": "2024-01-01T11:30:00Z"
}
```

### Error Codes
- `INVALID_REQUEST`: Malformed request
- `UNAUTHORIZED`: Missing/invalid authentication
- `FORBIDDEN`: Insufficient permissions
- `NOT_FOUND`: Resource doesn't exist
- `RATE_LIMITED`: Too many requests
- `BID_TOO_LOW`: Bid amount insufficient
- `AUCTION_ENDED`: Auction no longer accepting bids
- `INTERNAL_ERROR`: Server error

## Rate Limiting

### Per-User Limits
- Bid placement: 5 bids/second, burst to 20
- Auction creation: 10 auctions/hour
- API calls: 100 requests/minute

### Per-IP Limits
- Anonymous requests: 50 requests/minute
- Authentication attempts: 5 attempts/minute

### Per-Auction Limits
- Total bids: 100 bids/second across all users
- Watchers: 1,000 concurrent watchers

## Data Types

### Monetary Values
- All amounts in USD cents (integers)
- Example: $1.50 = 150

### Timestamps
- ISO 8601 format with timezone
- Example: `2024-01-01T11:30:00Z`

### Identifiers
- UUID strings for all entity IDs
- Example: `550e8400-e29b-41d4-a716-446655440000`

### Sequence Numbers
- 64-bit integers for ordering
- Monotonic per auction for bid ordering

## Security Considerations

### Authentication
- JWT tokens with 1-hour expiration
- Refresh tokens for session management
- Secure token storage required

### Authorization
- Role-based access control (BUYER, SELLER, ADMIN)
- Resource ownership validation
- Permission checks on all operations

### Input Validation
- All inputs validated server-side
- SQL injection prevention
- XSS protection
- File upload restrictions

### Rate Limiting
- Distributed rate limiting with Redis
- Burst allowance for legitimate traffic spikes
- Automatic blocking of abusive patterns

## Performance Characteristics

### Latency Targets
- Bid placement: <10ms P95
- Auction listing: <100ms P95
- Data retrieval: <50ms P95

### Throughput Targets
- 10,000 bids/second sustained
- 100,000 auction views/minute
- 1M concurrent WebSocket connections

### Caching Strategy
- Auction data: 5-minute TTL
- Bid history: 1-hour TTL
- Reference data: 24-hour TTL

## Versioning

### API Versioning
- URL-based versioning: `/v1/`
- Backward compatibility maintained
- Deprecation notices in headers

### Breaking Changes
- New major version for breaking changes
- 12-month support for deprecated versions
- Migration guides provided

## Testing

### Sandbox Environment
- Full API available for testing
- Mock payment processing
- Isolated data environment

### Test Data
- Sample auctions and users
- Realistic bid patterns
- Performance testing scenarios

## Support

### Documentation
- OpenAPI/Swagger specification available
- Interactive API explorer
- Code examples in multiple languages

### Developer Portal
- API key management
- Usage analytics
- Webhook configuration

### Support Channels
- Email: api-support@auctionflow.com
- Slack: #api-support
- Documentation: https://docs.auctionflow.com

---

*This specification provides a comprehensive and easy-to-understand guide to the AuctionFlow API. For the latest updates, refer to the official documentation.*