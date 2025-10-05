# WebSocket and Security Fix

## Issues Fixed

### 1. 403 Forbidden on `/api/v1/auctions`
**Problem**: The auctions endpoint was not in the `permitAll()` list in SecurityConfig, causing 403 errors.

**Fix**: Added `/api/v1/auctions` and `/api/v1/auctions/**` to the permitted endpoints in `SecurityConfig.java`:
```java
.requestMatchers(
    "/api/v1/auth/**",
    "/api/v1/reference/**",
    "/api/v1/users",
    "/api/v1/auctions",      // NEW
    "/api/v1/auctions/**",   // NEW
    "/oauth2/**",
    "/login/**",
    "/actuator/**",
    "/ws/**",                // NEW - for WebSocket
    "/",
    "/ui/**",
    "/assets/**",
    "/static/**",
    "/images/**",
    "/favicon.ico"
).permitAll()
```

### 2. WebSocket Connection Failing
**Problem**: No WebSocket endpoint was configured, causing continuous reconnection attempts.

**Fixes**:
1. **Added WebSocket dependency** to `auction-api/build.gradle`:
   ```gradle
   implementation 'org.springframework.boot:spring-boot-starter-websocket'
   ```

2. **Created WebSocketConfig** (`auction-api/src/main/java/com/auctionflow/api/config/WebSocketConfig.java`):
   - Registers `/ws` endpoint
   - Allows all origins (for development)

3. **Created AuctionWebSocketHandler** (`auction-api/src/main/java/com/auctionflow/api/config/AuctionWebSocketHandler.java`):
   - Handles WebSocket connections
   - Supports subscribe/unsubscribe to auction updates
   - Provides broadcast functionality for real-time updates

4. **Added `/ws/**` to SecurityConfig** permitAll list

## How WebSocket Works

### Frontend Usage
The frontend automatically connects to WebSocket on page load:
```typescript
// Connects to ws://localhost:8080/ws
websocketService.connect();

// Subscribe to auction updates
websocketService.subscribe(auctionId);

// Listen for messages
websocketService.onMessage((message) => {
  // Handle bid updates, auction status changes, etc.
});
```

### Backend Handler
The `AuctionWebSocketHandler` manages:
- **Connection lifecycle**: Tracks active sessions
- **Subscriptions**: Maps sessions to auction IDs
- **Broadcasting**: Sends updates to all subscribed clients

### Message Format
```json
{
  "type": "subscribe",
  "auctionId": "auction-123"
}
```

Response messages:
```json
{
  "type": "bid_placed",
  "auctionId": "auction-123",
  "data": {
    "amount": 150.00,
    "bidder": "user-456",
    "timestamp": "2024-01-01T10:30:00Z"
  }
}
```

## Deployment

Run the quick update script:
```bash
chmod +x quick-update.sh
./quick-update.sh
```

Or manually:
```bash
# Stop container
docker compose stop auction-api

# Build with new dependencies
./gradlew :auction-api:build -x test

# Rebuild Docker image
docker compose build --no-cache auction-api

# Start container
docker compose up -d auction-api
```

## Testing

### 1. Check UI
Open http://localhost:8080/ui/ and verify:
- ✅ No "Reconnecting..." alert
- ✅ Auctions list loads
- ✅ No 403 errors in console

### 2. Test WebSocket Connection
Open browser DevTools > Network > WS tab:
- Should see connection to `ws://localhost:8080/ws`
- Status: 101 Switching Protocols (success)

### 3. Test Auction Endpoint
```bash
curl http://localhost:8080/api/v1/auctions
```
Should return 200 with auction list (or empty array).

## Next Steps

### Integrate Real-time Updates
To send bid updates to connected clients, inject the handler into your bid processing:

```java
@Autowired
private AuctionWebSocketHandler webSocketHandler;

public void processBid(Bid bid) {
    // Process bid...
    
    // Broadcast to subscribers
    Map<String, Object> message = Map.of(
        "type", "bid_placed",
        "auctionId", bid.getAuctionId(),
        "data", Map.of(
            "amount", bid.getAmount(),
            "bidder", bid.getBidderId(),
            "timestamp", bid.getTimestamp()
        )
    );
    webSocketHandler.broadcastToAuction(bid.getAuctionId(), message);
}
```

### Security Considerations
For production:
1. **Restrict origins** in WebSocketConfig:
   ```java
   .setAllowedOrigins("https://yourdomain.com")
   ```

2. **Add authentication** to WebSocket connections:
   - Validate JWT token in handshake
   - Store user info with session

3. **Rate limiting** on WebSocket messages

4. **Require authentication** for auction endpoints (remove from permitAll)

## Troubleshooting

### WebSocket still not connecting
1. Check logs: `docker compose logs auction-api | grep WebSocket`
2. Verify endpoint: `curl -i http://localhost:8080/ws` (should return 400, not 404)
3. Check browser console for specific error messages

### 403 on auctions endpoint
1. Verify SecurityConfig changes were deployed
2. Check if profile is active: `!ui-only` profile should be active
3. Test with curl: `curl -v http://localhost:8080/api/v1/auctions`

### Changes not taking effect
1. Ensure you rebuilt: `./gradlew :auction-api:build -x test`
2. Rebuild Docker image: `docker compose build --no-cache auction-api`
3. Restart container: `docker compose restart auction-api`
