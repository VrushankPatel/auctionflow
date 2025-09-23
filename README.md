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

- **Bid Latency**: <10ms average, <100ms p99 response time for bid acceptance (optimized with aggregate caching, read model caching, and zero-allocation hot paths).
- **Throughput**: 10,000+ bids per second sustained (improved with adaptive batch processing, efficient priority queues, and async processing).
- **Concurrency**: Support for 1 million concurrent watchers with real-time notifications.
- **Query Performance**: O(1) highest bid lookups with pre-computed fields and caching.

## Recent Optimizations

- Optimized database connection pool (50 max connections) for higher throughput
- Configured async thread pools (20-50 threads) for concurrent bid processing
- Enhanced Redis connection pooling for fast caching
- Implemented rate limiting and circuit breakers for stability
- Fixed compilation issues and updated to Spring Boot 3 compatibility
- Added aggregate caching to reduce event store loads
- Implemented adaptive batch processing for bid queues
- Added search functionality for auctions with full-text search
- Introduced item management API for complete auction lifecycle
- Added currentHighestBid fields to Auction entity for O(1) highest bid queries
- Implemented read model caching with cache eviction on bid placement
- Added event-driven read model updates for real-time consistency
- Zero-allocation hot paths with object pooling for BidValidator and Bid objects
- Server-assigned timestamps and sequence numbers for deterministic ordering
- Price-time priority with efficient priority queue (PriorityBlockingQueue)
- Hashed wheel timer for million+ concurrent auction timers
- Anti-snipe extensions with configurable policies
- Automated bidding strategies (sniping prevention, optimal timing, budget optimization, reinforcement learning)
- Real-time notifications via Redis Pub/Sub and Kafka WebSocket gateway
- CQRS/Event Sourcing for horizontal scalability and audit trails

## Bid Processing Optimizations

- **Asynchronous Bid Processing**: Bid placement returns immediately with optimistic acceptance, processing validation and persistence asynchronously to reduce response latency.
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
- **Proxy and Automated Bidding**: Integrated automated bidding strategies (sniping prevention, optimal timing, budget optimization, reinforcement learning) with async processing to maintain low latency.
- **Adaptive Batched Bid Processing**: Processes queued bids with adaptive batch sizing based on queue load (1-20 bids per batch) to balance latency and throughput in high-frequency scenarios.
- **Optimized Bid Queue Comparator**: Uses long-based comparison for Money amounts in priority queue to ensure correct price-time priority ordering with minimal latency.
- **Thread Safety Enforcement**: Single-writer per auction enforced via sharding; concurrent access prevented at command bus level.
- **Enhanced Money Utilities**: Refactored Money class to use long internally for fast arithmetic and comparisons, reducing BigDecimal overhead in hot paths. Added toBigDecimal() for compatibility.
- **Fine-Grained Locking**: Separate lock keys for proxy bidding ("auction:proxy:") to reduce contention with main bid processing.
- **High Throughput**: Supports 10,000+ bids/sec through optimized aggregate reconstruction and event-driven architecture.
- **Error Handling**: Proper exception handling in bid placement for accurate acceptance status.
- **Batch Event Saves**: Multiple bid events saved in single DB transaction to reduce round trips.
- **Aggregate State Sharing**: Proxy bidding uses shared aggregate state to avoid reloading events, improving performance.
- **Aggregate Caching**: In-memory caching of aggregate state to reduce event store loads and improve reconstruction performance.
- **Batched Proxy Bidding**: Proxy and automated bids are processed in batches with single event store saves and Kafka publishes to reduce latency.
- **Optimized DB Updates**: Proxy bid status and current bid updates are batched to minimize database round trips.

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
- `GET /api/v1/auctions` - List active auctions (with optional category, seller, and search query filters)
- `GET /api/v1/auctions/{id}` - Get auction details
- `POST /api/v1/auctions/{id}/bids` - Place a bid
- `POST /api/v1/auctions/{id}/buy-now` - Buy auction immediately
- `POST /api/v1/auctions/{id}/offers` - Make an offer
- `POST /api/v1/auctions/{id}/commits` - Commit bid for sealed auctions
- `POST /api/v1/auctions/{id}/reveals` - Reveal bid for sealed auctions
- `POST /api/v1/auctions/{id}/watch` - Add auction to watchlist
- `DELETE /api/v1/auctions/{id}/watch` - Remove auction from watchlist
- `POST /api/v1/auctions/{id}/close` - Admin force close auction
- `POST /api/v1/users` - Create a new user
- `GET /api/v1/users/{id}/bids` - Get user bid history
- `POST /api/v1/payments/webhook` - Payment provider webhook
- `GET /api/v1/reference/categories` - Get item categories
- `GET /api/v1/reference/bid-increments` - Get bid increment strategies
- `GET /api/v1/reference/auction-types` - Get auction types
- `GET /api/v1/reference/extension-policies` - Get extension policies
- `POST /api/v1/automated-bidding/strategies` - Create automated bidding strategy
- `GET /api/v1/automated-bidding/strategies` - List user strategies
- `DELETE /api/v1/automated-bidding/strategies/{id}` - Deactivate strategy
- `POST /api/v1/items` - Create a new item
- `GET /api/v1/items` - List items (with optional seller filter)
- `GET /api/v1/items/{id}` - Get item details
- `PUT /api/v1/items/{id}` - Update item
- `DELETE /api/v1/items/{id}` - Delete item

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.