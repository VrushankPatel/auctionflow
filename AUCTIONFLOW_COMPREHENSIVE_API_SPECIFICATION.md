# AuctionFlow API Specification

## Overview

AuctionFlow is a high-performance auction backend API built with Spring Boot, implementing CQRS/Event Sourcing for real-time bidding with sub-100ms latency. This API specification provides comprehensive documentation for all endpoints, request/response formats, authentication, and error handling.

## Base URL

```
https://api.auctionflow.com
```

## Authentication

All API requests require authentication using JWT tokens or OAuth2 access tokens.

### Headers

```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### Authentication Endpoints

#### POST /api/v1/auth/login
Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "username": "string",
  "password": "string"
}
```

**Response:**
```json
{
  "token": "jwt_token_here",
  "expiresIn": 3600,
  "user": {
    "id": "string",
    "username": "string",
    "email": "string"
  }
}
```

#### POST /api/v1/auth/register
Register a new user account.

**Request Body:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string"
}
```

**Response:**
```json
{
  "user": {
    "id": "string",
    "username": "string",
    "email": "string"
  },
  "message": "User registered successfully"
}
```

## Auction Management

### Create Auction

**POST /api/v1/auctions**

Create a new auction.

**Request Body:**
```json
{
  "item": {
    "title": "string",
    "description": "string",
    "category": "string",
    "images": ["string"],
    "metadata": {}
  },
  "auctionType": "ENGLISH",
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-07T10:00:00Z",
  "reservePrice": 100.00,
  "buyNowPrice": 500.00,
  "incrementStrategy": "FIXED",
  "extensionPolicy": "NONE"
}
```

**Response:**
```json
{
  "auctionId": "string",
  "status": "CREATED",
  "createdAt": "2024-01-01T09:00:00Z"
}
```

### List Auctions

**GET /api/v1/auctions**

Retrieve auctions with optional filtering.

**Query Parameters:**
- `status`: Auction status (ACTIVE, CLOSED, etc.)
- `category`: Item category
- `seller`: Seller ID
- `search`: Search term
- `page`: Page number (default: 0)
- `size`: Page size (default: 20)
- `sort`: Sort field (default: startTime)

**Response:**
```json
{
  "auctions": [
    {
      "id": "string",
      "item": {
        "title": "string",
        "description": "string",
        "category": "string"
      },
      "seller": {
        "id": "string",
        "name": "string"
      },
      "currentHighestBid": 150.00,
      "bidCount": 5,
      "startTime": "2024-01-01T10:00:00Z",
      "endTime": "2024-01-07T10:00:00Z",
      "status": "ACTIVE"
    }
  ],
  "totalElements": 100,
  "totalPages": 5,
  "currentPage": 0
}
```

### Get Auction Details

**GET /api/v1/auctions/{auctionId}**

Get detailed information about a specific auction.

**Response:**
```json
{
  "id": "string",
  "item": {
    "title": "string",
    "description": "string",
    "category": "string",
    "images": ["string"],
    "metadata": {}
  },
  "seller": {
    "id": "string",
    "name": "string",
    "rating": 4.8
  },
  "auctionType": "ENGLISH",
  "startTime": "2024-01-01T10:00:00Z",
  "endTime": "2024-01-07T10:00:00Z",
  "reservePrice": 100.00,
  "buyNowPrice": 500.00,
  "currentHighestBid": 150.00,
  "highestBidder": "string",
  "bidCount": 5,
  "status": "ACTIVE",
  "watchers": 25
}
```

### Update Auction

**PUT /api/v1/auctions/{auctionId}**

Update auction details (seller only).

**Request Body:**
```json
{
  "description": "Updated description",
  "buyNowPrice": 550.00
}
```

**Response:**
```json
{
  "auctionId": "string",
  "updatedAt": "2024-01-01T11:00:00Z"
}
```

## Bidding

### Place Bid

**POST /api/v1/auctions/{auctionId}/bids**

Place a bid on an auction.

**Request Body:**
```json
{
  "amount": 160.00,
  "maxBid": 200.00,
  "bidType": "MANUAL"
}
```

**Response:**
```json
{
  "bidId": "string",
  "auctionId": "string",
  "amount": 160.00,
  "status": "ACCEPTED",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

### Bulk Place Bids

**POST /api/v1/auctions/{auctionId}/bulk-bids**

Place multiple bids at once.

**Request Body:**
```json
{
  "bids": [
    {
      "amount": 170.00,
      "bidType": "AUTO"
    },
    {
      "amount": 180.00,
      "bidType": "AUTO"
    }
  ]
}
```

**Response:**
```json
{
  "successfulBids": 2,
  "failedBids": 0,
  "bids": [
    {
      "bidId": "string",
      "amount": 170.00,
      "status": "ACCEPTED"
    }
  ]
}
```

### Get Bid History

**GET /api/v1/auctions/{auctionId}/bids**

Get bid history for an auction.

**Query Parameters:**
- `page`: Page number
- `size`: Page size

**Response:**
```json
{
  "bids": [
    {
      "id": "string",
      "bidder": {
        "id": "string",
        "name": "string"
      },
      "amount": 160.00,
      "timestamp": "2024-01-01T12:00:00Z",
      "bidType": "MANUAL"
    }
  ],
  "totalElements": 10
}
```

## Watchlist

### Add to Watchlist

**POST /api/v1/auctions/{auctionId}/watch**

Add auction to user's watchlist.

**Response:**
```json
{
  "auctionId": "string",
  "watchedAt": "2024-01-01T13:00:00Z"
}
```

### Remove from Watchlist

**DELETE /api/v1/auctions/{auctionId}/watch**

Remove auction from user's watchlist.

**Response:**
```json
{
  "auctionId": "string",
  "message": "Removed from watchlist"
}
```

### Get Watchlist

**GET /api/v1/user/watchlist**

Get user's watchlist.

**Response:**
```json
{
  "auctions": [
    {
      "id": "string",
      "title": "string",
      "currentBid": 150.00,
      "endTime": "2024-01-07T10:00:00Z"
    }
  ]
}
```

## Buy Now

### Immediate Purchase

**POST /api/v1/auctions/{auctionId}/buy-now**

Purchase auction immediately at buy-now price.

**Response:**
```json
{
  "auctionId": "string",
  "purchasePrice": 500.00,
  "status": "SOLD",
  "transactionId": "string"
}
```

## User Management

### Get User Profile

**GET /api/v1/users/{userId}**

Get user profile information.

**Response:**
```json
{
  "id": "string",
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "rating": 4.8,
  "memberSince": "2023-01-01T00:00:00Z"
}
```

### Update Profile

**PUT /api/v1/users/{userId}**

Update user profile.

**Request Body:**
```json
{
  "firstName": "Updated First Name",
  "lastName": "Updated Last Name"
}
```

**Response:**
```json
{
  "userId": "string",
  "updatedAt": "2024-01-01T14:00:00Z"
}
```

## Item Management

### Create Item

**POST /api/v1/items**

Create a new item for auction.

**Request Body:**
```json
{
  "title": "string",
  "description": "string",
  "category": "string",
  "images": ["string"],
  "metadata": {}
}
```

**Response:**
```json
{
  "itemId": "string",
  "createdAt": "2024-01-01T15:00:00Z"
}
```

### Get Item

**GET /api/v1/items/{itemId}**

Get item details.

**Response:**
```json
{
  "id": "string",
  "title": "string",
  "description": "string",
  "category": "string",
  "images": ["string"],
  "metadata": {},
  "owner": "string"
}
```

## Automated Bidding

### Create Bidding Strategy

**POST /api/v1/bidding-strategies**

Create an automated bidding strategy.

**Request Body:**
```json
{
  "auctionId": "string",
  "maxBid": 300.00,
  "increment": 10.00,
  "strategyType": "AGGRESSIVE"
}
```

**Response:**
```json
{
  "strategyId": "string",
  "status": "ACTIVE"
}
```

### Get Bidding Strategies

**GET /api/v1/bidding-strategies**

Get user's bidding strategies.

**Response:**
```json
{
  "strategies": [
    {
      "id": "string",
      "auctionId": "string",
      "maxBid": 300.00,
      "currentBid": 180.00,
      "status": "ACTIVE"
    }
  ]
}
```

## Payments

### Process Payment

**POST /api/v1/payments**

Process payment for won auction.

**Request Body:**
```json
{
  "auctionId": "string",
  "amount": 200.00,
  "paymentMethod": {
    "type": "CREDIT_CARD",
    "token": "string"
  }
}
```

**Response:**
```json
{
  "paymentId": "string",
  "status": "COMPLETED",
  "transactionId": "string"
}
```

### Payment Webhook

**POST /api/v1/payments/webhook**

Handle payment provider webhooks.

**Headers:**
```
X-Webhook-Signature: signature_here
```

**Request Body:**
```json
{
  "eventType": "PAYMENT_SUCCEEDED",
  "paymentId": "string",
  "amount": 200.00,
  "metadata": {}
}
```

**Response:**
```json
{
  "status": "PROCESSED"
}
```

## Reference Data

### Get Categories

**GET /api/v1/reference/categories**

Get available item categories.

**Response:**
```json
{
  "categories": [
    {
      "id": "string",
      "name": "string",
      "description": "string"
    }
  ]
}
```

### Get Bid Increments

**GET /api/v1/reference/bid-increments**

Get bid increment rules.

**Response:**
```json
{
  "increments": [
    {
      "minPrice": 0.00,
      "maxPrice": 100.00,
      "increment": 5.00
    }
  ]
}
```

## Real-time Notifications

### WebSocket Connection

**WebSocket /ws/notifications**

Connect to real-time notifications.

**Authentication:** Include JWT token in connection headers.

**Message Types:**

#### Bid Update
```json
{
  "type": "BID_UPDATE",
  "auctionId": "string",
  "bid": {
    "amount": 170.00,
    "bidder": "string",
    "timestamp": "2024-01-01T16:00:00Z"
  }
}
```

#### Auction Ended
```json
{
  "type": "AUCTION_ENDED",
  "auctionId": "string",
  "winner": "string",
  "finalPrice": 200.00
}
```

## Disputes

### Create Dispute

**POST /api/v1/disputes**

Create a dispute for an auction.

**Request Body:**
```json
{
  "auctionId": "string",
  "reason": "ITEM_NOT_AS_DESCRIBED",
  "description": "Item arrived damaged"
}
```

**Response:**
```json
{
  "disputeId": "string",
  "status": "OPEN",
  "createdAt": "2024-01-01T17:00:00Z"
}
```

### Get Disputes

**GET /api/v1/disputes**

Get user's disputes.

**Response:**
```json
{
  "disputes": [
    {
      "id": "string",
      "auctionId": "string",
      "reason": "string",
      "status": "OPEN",
      "createdAt": "2024-01-01T17:00:00Z"
    }
  ]
}
```

## Auction Templates

### Create Template

**POST /api/v1/auction-templates**

Create an auction template.

**Request Body:**
```json
{
  "name": "Standard Auction",
  "description": "Template for standard auctions",
  "templateData": {
    "auctionType": "ENGLISH",
    "duration": 604800,
    "reservePrice": 100.00
  },
  "isPublic": false
}
```

**Response:**
```json
{
  "templateId": "string",
  "createdAt": "2024-01-01T18:00:00Z"
}
```

### Get Templates

**GET /api/v1/auction-templates**

Get available auction templates.

**Response:**
```json
{
  "templates": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "isPublic": true,
      "creator": "string"
    }
  ]
}
```

## Error Handling

All API responses follow a consistent error format:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request parameters",
    "details": {
      "field": "amount",
      "issue": "Must be greater than current bid"
    }
  },
  "timestamp": "2024-01-01T19:00:00Z"
}
```

### Common Error Codes

- `400 BAD_REQUEST`: Invalid request parameters
- `401 UNAUTHORIZED`: Authentication required
- `403 FORBIDDEN`: Insufficient permissions
- `404 NOT_FOUND`: Resource not found
- `409 CONFLICT`: Resource conflict (e.g., bid too low)
- `429 TOO_MANY_REQUESTS`: Rate limit exceeded
- `500 INTERNAL_SERVER_ERROR`: Server error

## Rate Limiting

API endpoints are rate limited to prevent abuse:

- Bid placement: 5 bids per minute per user
- Auction creation: 10 auctions per hour per user
- General API calls: 1000 requests per hour per user

Rate limit headers are included in responses:

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1640995200
```

## Pagination

List endpoints support pagination with the following parameters:

- `page`: Page number (0-based, default: 0)
- `size`: Page size (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., `createdAt,desc`)

Response includes pagination metadata:

```json
{
  "data": [...],
  "page": {
    "number": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

## Versioning

API versioning is handled through URL paths:

- Current version: v1
- Future versions: v2, v3, etc.

Breaking changes will be introduced in new versions with advance notice.

## SDKs and Libraries

Official SDKs are available for:

- JavaScript/Node.js
- Python
- Java
- Go

## Support

For API support, contact:

- Email: api-support@auctionflow.com
- Documentation: https://docs.auctionflow.com
- Status Page: https://status.auctionflow.com