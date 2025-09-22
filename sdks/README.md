# Auction Flow API Client SDKs

This directory contains client SDKs for the Auction Flow REST API in multiple languages.

## Supported Languages

- **Java**: Feign-based client with type safety
- **JavaScript/TypeScript**: Axios-based client with TypeScript types
- **Python**: Requests-based client with Pydantic models
- **Go**: Resty-based client with struct types

## Features

All SDKs include:
- **Authentication**: JWT token handling
- **Retry Logic**: Exponential backoff for failed requests
- **Type Safety**: Strongly typed requests and responses
- **Error Handling**: Proper error propagation

## Usage

### Java

```java
AuctionFlowClient client = new AuctionFlowClient("http://localhost:8080");

AuthResponse auth = client.login("username", "password");
client.createAuction(new CreateAuctionRequest(...));
```

### JavaScript/TypeScript

```typescript
const client = new AuctionFlowClient("http://localhost:8080");

const auth = await client.login({ username: "user", password: "pass" });
await client.createAuction(request);
```

### Python

```python
client = AuctionFlowClient("http://localhost:8080")

auth = client.login(AuthRequest(username="user", password="pass"))
client.create_auction(CreateAuctionRequest(...))
```

### Go

```go
client := auctionflow.NewClient("http://localhost:8080")

auth, err := client.Login(models.AuthRequest{Username: "user", Password: "pass"})
err = client.CreateAuction(models.CreateAuctionRequest{...})
```

## Building

Each SDK has its own build system:
- Java: `./gradlew build`
- JS/TS: `npm run build`
- Python: `pip install -e .`
- Go: `go build`