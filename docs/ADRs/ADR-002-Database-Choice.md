# ADR-002: Choose PostgreSQL as Primary Database

## Status
Accepted

## Context
The system requires ACID transactions for bid placement, auction state management, and payment processing. We need support for complex queries, JSON storage, partitioning, and high availability. The database must handle high write throughput while maintaining data integrity.

## Decision
We will use PostgreSQL as the primary database for:
- Authoritative auction and bid state
- User and seller data
- Audit trails and event metadata
- Reference data and configurations

Key PostgreSQL features we'll leverage:
- SERIALIZABLE isolation for bid transactions
- JSONB for flexible metadata storage
- Partitioning by auction_id for scalability
- Read replicas for query optimization
- Row-level security for multi-tenancy

## Consequences
### Positive
- **ACID Compliance**: Strong consistency guarantees for critical operations
- **Rich Features**: JSON support, advanced indexing, partitioning
- **Ecosystem**: Mature tooling, drivers, and community support
- **Performance**: Excellent for mixed read/write workloads
- **Reliability**: Proven track record in production systems

### Negative
- **Operational Complexity**: Requires DBA expertise for tuning and maintenance
- **Scaling Limitations**: Vertical scaling may be needed for extreme loads
- **Cost**: Enterprise features may require licensing

### Mitigation
- Use read replicas to offload queries
- Implement partitioning strategy for horizontal scaling
- Comprehensive monitoring and alerting for performance issues

## Related
- ADR-001: CQRS Architecture
- ADR-003: Event Streaming
- ADR-005: Data Partitioning Strategy