# ADR-004: Implement Redis for High-Performance Caching

## Status
Accepted

## Context
The system requires sub-100ms response times for bid operations and auction queries. We need to cache frequently accessed data like current highest bids, auction details, and watcher lists. Cache must be consistent with authoritative database state and support high concurrency.

## Decision
We will use Redis as the primary caching layer:
- **Current Highest Bids**: Cache auction state with TTL and invalidation
- **Auction Details**: Hot auction metadata and status
- **Watcher Management**: Sets for efficient watcher tracking
- **Rate Limiting**: Token bucket implementation for abuse prevention
- **Session Storage**: User session data and authentication tokens

Caching strategy:
- Write-through for critical data (bids)
- Cache-aside for read-heavy data
- Optimistic fallback to database on cache misses
- Redis Pub/Sub for real-time invalidation

## Consequences
### Positive
- **Performance**: Sub-millisecond access to cached data
- **Scalability**: Horizontal scaling with Redis Cluster
- **Data Structures**: Rich types (sets, sorted sets, hashes)
- **Persistence**: Optional persistence for durability
- **Atomic Operations**: Lua scripting for complex operations

### Negative
- **Consistency**: Cache/database synchronization challenges
- **Memory Management**: Careful TTL and eviction policies needed
- **Operational Complexity**: Redis cluster management and monitoring
- **Cost**: Memory-intensive for large datasets

### Mitigation
- Implement cache invalidation strategies
- Use Redis Sentinel for high availability
- Monitor cache hit rates and performance metrics
- Fallback to database when cache is unavailable

## Related
- ADR-002: Database Choice
- ADR-005: Data Partitioning Strategy
- ADR-007: Rate Limiting Implementation