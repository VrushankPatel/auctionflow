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
- **Thread Safety**: Distributed locks per auction prevent race conditions; optimistic concurrency with retries ensures consistency.
- **Zero-Allocation Hot Paths**: Minimizes object creation in bid validation and event emission; reuses command data to reduce GC pressure.
- **Asynchronous Processing**: Bid placement is asynchronous with immediate response; proxy and automated bidding decoupled to maintain low latency.
- **High Throughput**: Supports 10,000+ bids/sec through optimized aggregate reconstruction and event-driven architecture.

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

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.