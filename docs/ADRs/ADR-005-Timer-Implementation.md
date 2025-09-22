# ADR-005: Use Hashed Wheel Timer for Auction Scheduling

## Status
Accepted

## Context
The system must schedule millions of auction close events with precise timing. Timers need to handle anti-sniping extensions, retries, and persistence across restarts. Traditional timer implementations don't scale well with high timer counts.

## Decision
We will implement a hashed wheel timer based on Netty's design:
- **Wheel Structure**: Hierarchical timing wheels for different time scales
- **Tick Duration**: 100ms resolution balancing accuracy and overhead
- **Capacity**: Support for millions of concurrent timers
- **Persistence**: Durable scheduled job records in database
- **Restart Recovery**: Reconcile timers from database on startup

Timer responsibilities:
- Auction close scheduling and extensions
- Anti-sniping policy enforcement
- Retry logic for failed operations
- Idempotent execution with database transactions

## Consequences
### Positive
- **Scalability**: Efficient O(1) timer management
- **Memory Efficient**: Low memory footprint per timer
- **Precision**: Millisecond-level timing accuracy
- **Performance**: Minimal CPU overhead for timer operations

### Negative
- **Complexity**: Custom implementation required
- **Precision Limits**: Not suitable for microsecond precision
- **Memory Bounds**: Fixed wheel size limits timer range
- **Persistence**: Additional complexity for durable timers

### Mitigation
- Use hierarchical wheels for different time ranges
- Implement persistence layer for critical timers
- Comprehensive testing for timer accuracy
- Monitoring and alerting for timer drift

## Related
- ADR-001: CQRS Architecture
- ADR-006: Anti-Sniping Policy
- ADR-008: Concurrency Control