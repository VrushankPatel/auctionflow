# Auction Flow API Reference

## Overview

The Auction Flow API provides REST endpoints for managing auctions, bids, users, and related operations. All endpoints require authentication via JWT tokens unless otherwise specified.

**Base URL:** `https://api.auctionflow.com/v1`

**Authentication:** Bearer token in `Authorization` header

**Content-Type:** `application/json`

## Items

### Create Item

Create a new item for auction.

**Endpoint:** `POST /items`

**Request Body:**
```json
{
  "title": "Vintage Watch",
  "description": "A beautiful vintage watch",
  "categoryId": "watches",
  "brand": "Rolex",
  "serialNumber": "123456",
  "images": ["url1", "url2"],
  "metadata": {"key": "value"}
}
```

**Response:**
```json
{
  "id": "uuid",
  "sellerId": "uuid",
  "title": "Vintage Watch",
  "description": "A beautiful vintage watch",
  "categoryId": "watches",
  "brand": "Rolex",
  "serialNumber": "123456",
  "images": ["url1", "url2"],
  "metadata": {"key": "value"}
}
```

### List Items

List items with optional seller filter.

**Endpoint:** `GET /items?sellerId={uuid}`

**Response:**
```json
[
  {
    "id": "uuid",
    "sellerId": "uuid",
    "title": "Vintage Watch",
    ...
  }
]
```

### Get Item

Get item details.

**Endpoint:** `GET /items/{id}`

### Update Item

Update item (seller only).

**Endpoint:** `PUT /items/{id}`

### Delete Item

Delete item (seller only).

**Endpoint:** `DELETE /items/{id}`

## Auctions

### Create Auction

Create a new auction.

**Endpoint:** `POST /auctions`

**Request Body:**
```json
{
  "item_id": "uuid",
  "type": "english_open",
  "start_ts": "2024-01-01T10:00:00Z",
  "end_ts": "2024-01-01T12:00:00Z",
  "reserve_price": 100.00,
  "buy_now_price": 500.00,
  "increment_strategy_id": "uuid",
  "extension_policy_id": "uuid"
}
```

**Response:**
```json
{
  "id": "uuid",
  "status": "created",
  "server_ts": "2024-01-01T09:00:00.123Z",
  "seq_no": 12345
}
```

### Update Auction

Update auction metadata before first bid.

**Endpoint:** `PATCH /auctions/{id}`

**Request Body:**
```json
{
  "title": "Updated Title",
  "description": "Updated description"
}
```

### List Auctions

Retrieve paginated list of auctions with filtering.

**Endpoint:** `GET /auctions`

**Query Parameters:**
- `status`: OPEN, CLOSED, CANCELLED
- `category`: Filter by category
- `sellerId`: Filter by seller
- `query`: Search in title and description
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)

**Response:**
```json
{
  "auctions": [
    {
      "id": "uuid",
      "item": {...},
      "current_highest_bid": {
        "amount": 150.00,
        "bidder_id": "uuid",
        "server_ts": "2024-01-01T10:30:00.456Z"
      },
      "status": "OPEN",
      "end_ts": "2024-01-01T12:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "total": 150
  }
}
```

### Get Auction Details

Retrieve detailed auction information.

**Endpoint:** `GET /auctions/{id}`

**Response:**
```json
{
  "id": "uuid",
  "item": {...},
  "seller": {...},
  "current_highest_bid": {...},
  "bids_count": 25,
  "watchers_count": 150,
  "status": "OPEN",
  "start_ts": "2024-01-01T10:00:00Z",
  "end_ts": "2024-01-01T12:00:00Z",
  "reserve_price": 100.00,
  "buy_now_price": 500.00
}
```

### Place Bid

Submit a bid for an auction.

**Endpoint:** `POST /auctions/{id}/bids`

**Request Body:**
```json
{
  "amount": 200.00,
  "idempotency_key": "optional-uuid"
}
```

**Response:**
```json
{
  "accepted": true,
  "reason": null,
  "server_ts": "2024-01-01T10:45:00.789Z",
  "seq_no": 12346,
  "current_highest": 200.00
}
```

### Place Bulk Bids

Submit multiple bids for an auction in a single request.

**Endpoint:** `POST /auctions/{id}/bulk-bids`

**Request Body:**
```json
[
  {
    "amount": 200.00,
    "idempotency_key": "optional-uuid-1"
  },
  {
    "amount": 210.00,
    "idempotency_key": "optional-uuid-2"
  }
]
```

**Response:**
```json
[
  {
    "accepted": true,
    "server_ts": "2024-01-01T10:45:00.789Z",
    "seq_no": 12346
  },
  {
    "accepted": true,
    "server_ts": "2024-01-01T10:45:01.000Z",
    "seq_no": 12347
  }
]
```

### Get Auction Bids

Retrieve historical bids for an auction.

**Endpoint:** `GET /auctions/{id}/bids`

**Query Parameters:**
- `page`: Page number
- `size`: Page size

**Response:**
```json
{
  "bids": [
    {
      "id": "uuid",
      "bidder_id": "uuid",
      "amount": 200.00,
      "server_ts": "2024-01-01T10:45:00.789Z",
      "seq_no": 12346,
      "accepted": true
    }
  ],
  "pagination": {...}
}
```

### Add Watcher

Add auction to user's watchlist.

**Endpoint:** `POST /auctions/{id}/watch`

### Remove Watcher

Remove auction from user's watchlist.

**Endpoint:** `DELETE /auctions/{id}/watch`

### Buy Now

Execute immediate purchase.

**Endpoint:** `POST /auctions/{id}/buy-now`

### Admin Close Auction

Force close an auction (admin only).

**Endpoint:** `POST /auctions/{id}/close`

## Users

### Create User

Register a new user.

**Endpoint:** `POST /users`

**Request Body:**
```json
{
  "email": "user@example.com",
  "display_name": "John Doe",
  "password": "secure_password"
}
```

### Get User Bids

Retrieve user's bid history.

**Endpoint:** `GET /users/{id}/bids`

## Payments

### Payment Webhook

Handle payment provider callbacks.

**Endpoint:** `POST /payments/webhook`

**Note:** Requires payment provider authentication

## Reference Data

### Get Categories

**Endpoint:** `GET /reference/categories`

### Get Bid Increments

**Endpoint:** `GET /reference/bid-increments`

### Get Auction Types

**Endpoint:** `GET /reference/auction-types`

### Get Extension Policies

**Endpoint:** `GET /reference/extension-policies`

## Error Responses

All endpoints may return the following error responses:

**400 Bad Request:**
```json
{
  "error": "INVALID_REQUEST",
  "message": "Validation failed",
  "details": {...}
}
```

**401 Unauthorized:**
```json
{
  "error": "UNAUTHORIZED",
  "message": "Authentication required"
}
```

**403 Forbidden:**
```json
{
  "error": "FORBIDDEN",
  "message": "Insufficient permissions"
}
```

**404 Not Found:**
```json
{
  "error": "NOT_FOUND",
  "message": "Resource not found"
}
```

**429 Too Many Requests:**
```json
{
  "error": "RATE_LIMITED",
  "message": "Rate limit exceeded"
}
```

**500 Internal Server Error:**
```json
{
  "error": "INTERNAL_ERROR",
  "message": "An unexpected error occurred"
}
```

## Rate Limits

- Bid placement: 5 bids/sec per account (burst to 20)
- Auction creation: 10 auctions/hour per account
- General queries: 100 requests/minute per IP

## WebSocket Notifications

Real-time updates available via WebSocket at `/ws/notifications`

**Events:**
- `bid_placed`: New bid on watched auction
- `auction_extended`: Auction end time extended
- `auction_closed`: Auction ended with winner
- `auction_cancelled`: Auction cancelled

**Message Format:**
```json
{
  "event": "bid_placed",
  "auction_id": "uuid",
  "data": {...},
  "timestamp": "2024-01-01T10:45:00.789Z"
}
```