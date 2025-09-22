# Auction Flow Performance Tuning Guide

## Overview

This guide provides strategies for optimizing Auction Flow performance to meet the defined performance budgets and handle high-throughput auction scenarios.

## Performance Targets

| Operation | Target | Current Budget |
|-----------|--------|----------------|
| Bid Placement | <10ms average | <10ms |
| Auction Creation | <50ms average | <50ms |
| Auction Listing | <100ms average | <100ms |
| Auction Close | <50ms average | <50ms |
| Timer Scheduling | <5ms average | <5ms |

## Application Optimization

### JVM Tuning

#### Memory Configuration
```bash
# Production JVM settings
-Xms4g -Xmx8g \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:G1HeapRegionSize=16m \
-XX:InitiatingHeapOccupancyPercent=70 \
-XX:+PrintGCDetails \
-XX:+PrintGCTimeStamps \
-Xloggc:/var/log/auction-flow/gc.log
```

#### Garbage Collection Tuning
- **G1GC** for low-pause garbage collection
- **Heap sizing**: 4GB minimum, 8GB maximum for production
- **GC logging**: Enable for monitoring and tuning

### Thread Pool Configuration

```yaml
auction:
  async:
    core-pool-size: 20
    max-pool-size: 50
    queue-capacity: 1000
    thread-name-prefix: auction-async-

  bidding:
    thread-pool-size: 10
    queue-capacity: 500

  timer:
    thread-pool-size: 5
```

### Connection Pool Tuning

#### Database Connection Pool
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 20000
      idle-timeout: 300000
      max-lifetime: 1200000
      leak-detection-threshold: 60000
```

#### Redis Connection Pool
```yaml
spring:
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
        max-wait: -1ms
```

## Database Optimization

### Indexing Strategy

#### Core Indexes
```sql
-- Auction queries
CREATE INDEX idx_auctions_status_end_ts ON auctions(status, end_ts);
CREATE INDEX idx_auctions_seller_status ON auctions(seller_id, status);
CREATE INDEX idx_auctions_category_status ON auctions(category_id, status);

-- Bid queries
CREATE INDEX idx_bids_auction_seq ON bids(auction_id, seq_no DESC);
CREATE INDEX idx_bids_bidder_ts ON bids(bidder_id, server_ts DESC);
CREATE INDEX idx_bids_auction_amount ON bids(auction_id, amount DESC);

-- User queries
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_kyc_status ON users(kyc_status);
```

#### Partial Indexes
```sql
-- Active auctions only
CREATE INDEX idx_active_auctions_end_ts ON auctions(end_ts)
WHERE status = 'OPEN';

-- Recent bids
CREATE INDEX idx_recent_bids_ts ON bids(server_ts DESC)
WHERE server_ts > NOW() - INTERVAL '30 days';
```

### Query Optimization

#### Pagination
```sql
-- Efficient pagination with index
SELECT * FROM auctions
WHERE status = 'OPEN'
ORDER BY end_ts ASC
LIMIT 20 OFFSET 0;

-- Use cursor-based pagination for large datasets
SELECT * FROM bids
WHERE auction_id = $1 AND seq_no < $cursor
ORDER BY seq_no DESC
LIMIT 50;
```

#### Batch Operations
```sql
-- Bulk bid insertion
INSERT INTO bids (auction_id, bidder_id, amount, server_ts, seq_no)
VALUES ($1, $2, $3, $4, $5), ($6, $7, $8, $9, $10), ...;
```

### Partitioning Strategy

#### Table Partitioning
```sql
-- Partition bids by auction_id
CREATE TABLE bids_y2024m01 PARTITION OF bids
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

-- Partition auctions by creation date
CREATE TABLE auctions_y2024 PARTITION OF auctions
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

#### Index Partitioning
```sql
-- Partition indexes for better performance
CREATE INDEX idx_bids_auction_seq_y2024 ON bids_y2024(auction_id, seq_no DESC);
```

## Caching Optimization

### Redis Configuration

#### Memory Management
```redis.conf
# Memory settings
maxmemory 2gb
maxmemory-policy allkeys-lru

# Persistence
save 900 1
save 300 10
save 60 10000

# Performance
tcp-keepalive 300
timeout 300
```

#### Cache Key Design
```
auction:{id} -> Auction details (TTL: 5min)
auction:{id}:bids:top -> Top 10 bids (TTL: 1min)
auction:{id}:watchers -> Watcher set (TTL: 10min)
user:{id}:bids:recent -> Recent bids (TTL: 30min)
rate_limit:{user_id} -> Token bucket (TTL: 1hour)
```

### Cache Warming Strategy

```java
@Service
public class CacheWarmer {

    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void warmHotAuctions() {
        List<Auction> hotAuctions = auctionRepository.findHotAuctions();
        hotAuctions.forEach(auction -> {
            cacheService.cacheAuction(auction);
            cacheService.cacheTopBids(auction.getId());
        });
    }
}
```

### Cache Invalidation

#### Write-Through Pattern
```java
@Transactional
public void placeBid(Bid bid) {
    // Update database
    bidRepository.save(bid);

    // Update cache
    cacheService.updateHighestBid(bid.getAuctionId(), bid);

    // Publish event
    eventPublisher.publish(new BidPlacedEvent(bid));
}
```

## Kafka Optimization

### Producer Configuration
```yaml
spring:
  kafka:
    producer:
      acks: 1  # Balance durability and performance
      batch-size: 16384
      linger-ms: 5
      compression-type: lz4
      enable-idempotence: true
      retries: 10
      delivery-timeout-ms: 30000
```

### Consumer Configuration
```yaml
spring:
  kafka:
    consumer:
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 500
      fetch-min-bytes: 1024
      fetch-max-wait: 500ms
      concurrency: 5
```

### Topic Configuration
```bash
# Create optimized topics
kafka-topics --create \
  --topic auction-events \
  --partitions 12 \
  --replication-factor 3 \
  --config retention.ms=604800000 \
  --config segment.bytes=1073741824 \
  --config compression.type=lz4
```

## Timer Optimization

### Hashed Wheel Timer Tuning

```yaml
auction:
  timer:
    tick-duration: 50ms      # 50ms for higher precision
    wheel-size: 1024         # Support more concurrent timers
    max-pending-timers: 1000000
    persistence:
      batch-size: 100        # Batch persistence operations
      retry-attempts: 3
```

### Timer Distribution

#### Sharding Strategy
```java
public class TimerShardManager {

    private final int shardCount = 10;

    public int getShard(String auctionId) {
        return Math.abs(auctionId.hashCode()) % shardCount;
    }

    public TimerWheel getTimerForAuction(String auctionId) {
        int shard = getShard(auctionId);
        return timerWheels.get(shard);
    }
}
```

## Network Optimization

### HTTP/2 Configuration
```yaml
server:
  http2:
    enabled: true

  compression:
    enabled: true
    mime-types: text/html,text/xml,text/plain,text/css,text/javascript,application/javascript,application/json
    min-response-size: 1024
```

### Connection Pooling
```yaml
auction:
  http:
    client:
      max-connections: 100
      max-connections-per-route: 20
      connection-timeout: 5000ms
      read-timeout: 30000ms
```

## Monitoring and Profiling

### JMH Benchmarks

#### Bid Placement Benchmark
```java
@Benchmark
public void benchmarkBidPlacement() {
    BidRequest request = createBidRequest();
    bidService.placeBid(request);
}

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class AuctionFlowBenchmark {}
```

#### Database Query Benchmarks
```java
@Benchmark
public void benchmarkAuctionQuery() {
    auctionRepository.findByStatus(AuctionStatus.OPEN);
}
```

### Profiling Tools

#### Async Profiler
```bash
# CPU profiling
./profiler.sh -d 30 -f cpu_profile.html <pid>

# Memory profiling
./profiler.sh -d 30 -e alloc -f memory_profile.html <pid>
```

#### Java Flight Recorder
```bash
# Enable JFR
-XX:StartFlightRecording=duration=60s,filename=recording.jfr

# Analyze with Java Mission Control
jmc
```

## Load Testing

### JMeter Test Plan

#### Bid Storm Scenario
```xml
<ThreadGroup>
  <num_threads>100</num_threads>
  <ramp_time>10</ramp_time>
  <loop_count>100</loop_count>
  <HTTPRequest>
    <method>POST</method>
    <path>/api/v1/auctions/${auction_id}/bids</path>
    <body>{"amount": "${__Random(100,1000)}"}</body>
  </HTTPRequest>
</ThreadGroup>
```

#### Auction Listing Load Test
```xml
<ThreadGroup>
  <num_threads>50</num_threads>
  <ramp_time>5</ramp_time>
  <loop_count>200</loop_count>
  <HTTPRequest>
    <method>GET</method>
    <path>/api/v1/auctions?status=OPEN&amp;page=${__counter(false)}</path>
  </HTTPRequest>
</ThreadGroup>
```

### Gatling Simulation

```scala
class AuctionSimulation extends Simulation {

  val bidScenario = scenario("Bid Storm")
    .exec(http("Place Bid")
      .post("/api/v1/auctions/${auctionId}/bids")
      .body(StringBody("""{"amount": "${amount}"}"""))
      .check(status.is(200))
    )

  setUp(
    bidScenario.inject(
      rampUsers(1000).during(30.seconds),
      constantUsersPerSec(50).during(5.minutes)
    )
  ).protocols(httpProtocol)
}
```

## Scaling Strategies

### Horizontal Scaling

#### API Service Scaling
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: auction-api-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: auction-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

#### Database Read Scaling
```yaml
auction:
  datasource:
    read:
      - host: postgres-replica-1
        port: 5432
      - host: postgres-replica-2
        port: 5432
    write:
      host: postgres-primary
      port: 5432
```

### Vertical Scaling

#### Database Instance Sizing
- **CPU**: 8-16 cores for high concurrency
- **Memory**: 64-128GB for large working sets
- **Storage**: NVMe SSDs with 10k+ IOPS
- **Network**: 10Gbps for high-throughput scenarios

#### Cache Cluster Scaling
```yaml
# Redis Cluster configuration
redis:
  cluster:
    nodes:
      - host: redis-1:6379
      - host: redis-2:6379
      - host: redis-3:6379
    max-redirects: 3
```

## Performance Monitoring

### Key Metrics

#### Application Metrics
- **Bid Latency**: P50, P95, P99 response times
- **Throughput**: Requests per second by endpoint
- **Error Rate**: 4xx/5xx rates by endpoint
- **Resource Usage**: CPU, memory, thread utilization

#### Infrastructure Metrics
- **Database**: Connection pool usage, query latency, lock waits
- **Cache**: Hit rates, memory usage, eviction rates
- **Message Queue**: Producer/consumer lag, throughput
- **Network**: Bandwidth usage, connection counts

### Alerting Thresholds

```yaml
# Performance degradation alerts
- alert: BidLatencyDegraded
  expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{endpoint="placeBid"}[5m])) > 0.1
  for: 5m

- alert: DatabaseConnectionPoolHigh
  expr: jdbc_connections_active / jdbc_connections_max > 0.8
  for: 2m

- alert: CacheHitRateLow
  expr: rate(redis_keyspace_hits_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])) < 0.8
  for: 5m
```

## Continuous Optimization

### Performance Regression Testing

#### Automated Benchmarks
```bash
# Run JMH benchmarks in CI
./gradlew jmh

# Compare against baseline
./gradlew jmh -Pjmh.baseline=previous-version
```

#### Load Testing in CI/CD
```bash
# Run Gatling tests
./gradlew gatlingRun

# Performance gate
if [ $(cat results/simulation.log | grep "mean requests/sec" | awk '{print $4}') -lt 1000 ]; then
  echo "Performance regression detected"
  exit 1
fi
```

### Capacity Planning

#### Resource Forecasting
- **CPU**: Monitor usage patterns, plan for peak loads
- **Memory**: Track heap usage, plan for data growth
- **Storage**: Monitor I/O patterns, plan for retention
- **Network**: Track bandwidth usage, plan for scaling

#### Auto-scaling Policies
- **Scale Up**: When CPU > 70% for 5 minutes
- **Scale Down**: When CPU < 30% for 10 minutes
- **Scale Out**: When request queue depth > 100
- **Emergency Scaling**: When P95 latency > 200ms

## Best Practices

### Code Optimization
1. **Avoid N+1 Queries**: Use batch loading and JOINs
2. **Implement Caching**: Cache frequently accessed data
3. **Use Asynchronous Processing**: Offload heavy operations
4. **Optimize Data Structures**: Use efficient collections
5. **Profile Regularly**: Identify bottlenecks early

### Infrastructure Optimization
1. **Right-size Instances**: Match resources to workload
2. **Implement CDN**: Cache static assets
3. **Use Connection Pooling**: Reuse connections efficiently
4. **Monitor Everything**: Comprehensive observability
5. **Automate Scaling**: Respond to load automatically

### Operational Excellence
1. **Regular Load Testing**: Validate performance regularly
2. **Performance Budgets**: Set and monitor targets
3. **Incident Response**: Fast detection and resolution
4. **Continuous Improvement**: Learn from performance data
5. **Documentation**: Keep tuning guides updated