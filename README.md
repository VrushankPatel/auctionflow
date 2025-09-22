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

API documentation is available at [https://api.auctionflow.com/docs](https://api.auctionflow.com/docs) (placeholder).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

Licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.