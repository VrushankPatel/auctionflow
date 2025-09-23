# Auction Flow

A production-grade auction backend built with Spring Boot, designed for high-performance, real-time bidding with sub-100ms latency.

## Architecture Overview

Auction Flow implements a CQRS/Event Sourcing architecture to ensure correctness, durability, and horizontal scalability:

- **REST API Service**: Spring Boot application handling auction creation, bidding, and queries.
- **Persistent Store**: PostgreSQL for authoritative state and audit trails.
- **Fast Cache**: Redis for hot reads, current highest bids, and watcher management.
- **Event Stream**: Kafka for durable event logging, replay, and async processing.
- **Timer Service**: In-memory hashed wheel timer for auction scheduling, with persistent fallbacks.
- **Broadcaster**: Redis Pub/Sub or Kafka WebSocket gateway for real-time notifications.
- **Payment Integration**: Webhook-based integration with PCI-compliant providers.

## Performance Targets

- **Bid Latency**: <100ms p99 response time for bid acceptance.
- **Throughput**: 10,000 bids per second sustained.
- **Concurrency**: Support for 1 million concurrent watchers.

## Bid Processing Optimizations

- **Server-Assigned Timestamps and Sequence Numbers**: API layer assigns server timestamp and monotonic sequence number to each bid for deterministic ordering and client reconciliation.
- **Efficient Highest Bid Tracking**: Maintains current highest bid in aggregate state for O(1) lookups, avoiding O(n) scans of bid history.
- **Fair Ordering**: Uses sequence numbers for price-time priority, with tie-breaking by sequence number for simultaneous bids.
- **Bid Validation with Increments**: Integrated BidValidator enforces minimum bid increments and reserve prices for fair bidding.
- **Bid Queue Management**: Concurrent priority queue (PriorityBlockingQueue) for efficient bid ordering and processing, ensuring price-time priority with O(log n) insertion and O(1) peek operations, reducing contention in high-frequency scenarios.
- **Optimized Price-Time Priority Logic**: Refactored bid comparison logic with dedicated method for clarity and performance in determining highest priority bids.
- **Thread Safety**: Distributed locks per auction prevent race conditions; optimistic concurrency with retries ensures consistency.
- **Zero-Allocation Hot Paths**: Minimizes object creation in bid validation using thread-local ValidationResult pools and ObjectPool for BidValidator instances; reuses command data to reduce GC pressure. Added getFirstError() method to avoid list allocations in error paths. Bid objects now use object pooling with ArrayBlockingQueue for zero-allocation in high-frequency bidding.
- **Global Sequence Number Generation**: Uses Redis atomic increments for per-auction monotonic sequence numbers, ensuring fairness in distributed environments. Replaced local AtomicLong with distributed SequenceService.
- **Proper Bid Increment Strategy**: Proxy bidding uses configurable bid increment strategies instead of hardcoded values for accurate minimum bid calculations.
- **Precise Anti-Snipe Timing**: Anti-snipe extensions use bid command's server timestamp for accurate timing, preventing approximation errors.
- **Asynchronous Processing**: Bid placement is asynchronous with immediate response; proxy and automated bidding decoupled to maintain low latency via async execution.
- **Adaptive Batched Bid Processing**: Processes queued bids with adaptive batch sizing based on queue load (1-20 bids per batch) to balance latency and throughput in high-frequency scenarios.
- **Optimized Bid Queue Comparator**: Uses long-based comparison for Money amounts in priority queue to ensure correct price-time priority ordering with minimal latency.
- **Thread Safety Enforcement**: Single-writer per auction enforced via sharding; concurrent access prevented at command bus level.
- **Enhanced Money Utilities**: Refactored Money class to use long internally for fast arithmetic and comparisons, reducing BigDecimal overhead in hot paths. Added toBigDecimal() for compatibility.
- **Fine-Grained Locking**: Separate lock keys for proxy bidding ("auction:proxy:") to reduce contention with main bid processing.
- **High Throughput**: Supports 10,000+ bids/sec through optimized aggregate reconstruction and event-driven architecture.
- **Error Handling**: Proper exception handling in bid placement for accurate acceptance status.
- **Batch Event Saves**: Multiple bid events saved in single DB transaction to reduce round trips.
- **Aggregate State Sharing**: Proxy bidding uses shared aggregate state to avoid reloading events, improving performance.

## Modules

- **auction-core**: Core business logic, domain models, and auction rules.
- **auction-api**: REST API endpoints, controllers, and request/response handling.
- **auction-events**: Event sourcing infrastructure, event publishing, and replay.
- **auction-timers**: Auction scheduling, anti-sniping extensions, and timer management.
- **auction-notifications**: Real-time notifications via WebSocket/SSE.
- **auction-payments**: Payment processing, escrow, and webhook handling.
- **auction-analytics**: Data aggregation, reporting, and analytics.
- **auction-common**: Shared utilities, configurations, and common code.
- **auction-tests**: Testing utilities, fixtures, and integration test suites.

## Getting Started

### Prerequisites

- Java 17
- Gradle
- PostgreSQL
- Redis
- Kafka

### Building

```bash
./gradlew build
```

### Running

```bash
./gradlew bootRun
```

## API Documentation

API documentation is available at [https://api.auctionflow.com/docs](https://api.auctionflow.com/docs)

### Key Endpoints

- `POST /api/v1/auctions` - Create a new auction
- `GET /api/v1/auctions` - List active auctions
- `GET /api/v1/auctions/{id}` - Get auction details
- `POST /api/v1/auctions/{id}/bids` - Place a bid
- `POST /api/v1/auctions/{id}/buy-now` - Buy auction immediately
- `POST /api/v1/auctions/{id}/offers` - Make an offer
- `POST /api/v1/auctions/{id}/commits` - Commit bid for sealed auctions
- `POST /api/v1/auctions/{id}/reveals` - Reveal bid for sealed auctions

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.