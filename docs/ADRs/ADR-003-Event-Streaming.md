# ADR-003: Use Apache Kafka for Event Streaming

## Status
Accepted

## Context
The system generates high-volume events (bids, auction closes, notifications) that need to be processed asynchronously. We require durable event storage, replay capabilities, and fan-out to multiple consumers (notifications, analytics, payments). Events must be ordered and retained for audit and recovery.

## Decision
We will use Apache Kafka as the event streaming platform:
- Topics for different event types (bids, auctions, payments)
- Partitioning by auction_id for ordering guarantees
- Retention policies for audit and replay
- Consumer groups for parallel processing

Key usage patterns:
- Event sourcing: Reconstruct state from event streams
- CQRS: Event-driven updates to read models
- Real-time processing: Notifications and broadcaster
- Analytics: Event aggregation and reporting

## Consequences
### Positive
- **Durability**: Persistent event log with configurable retention
- **Scalability**: Horizontal scaling of producers and consumers
- **Ordering**: Partition-level ordering guarantees
- **Ecosystem**: Rich tooling for monitoring, management, and integration
- **Replay**: Ability to replay events for testing and recovery

### Negative
- **Operational Complexity**: Requires ZooKeeper/KRaft management
- **Latency**: Not optimized for ultra-low latency (<1ms) scenarios
- **Storage**: Event compaction and retention management
- **Learning Curve**: Understanding Kafka concepts and operations

### Mitigation
- Start with single-node Kafka for development
- Use Kafka Connect for external integrations
- Implement comprehensive monitoring and alerting
- Document operational procedures and runbooks

## Related
- ADR-001: CQRS Architecture
- ADR-004: Caching Strategy
- ADR-006: Notification Architecture