# Auction Flow Troubleshooting Guide

## Overview

This guide helps diagnose and resolve common issues in Auction Flow deployments. Issues are categorized by component and include symptoms, causes, and solutions.

## Application Issues

### High Bid Latency

**Symptoms:**
- Bid placement taking >100ms
- P99 latency exceeding performance budgets
- Users reporting slow response times

**Possible Causes:**
- Database connection pool exhausted
- Redis cache misses causing DB load
- High CPU usage on application servers
- Network latency to database/cache

**Solutions:**
1. **Check Database Connections:**
   ```bash
   # Monitor connection pool usage
   curl http://localhost:8080/actuator/metrics | grep hikaricp

   # Increase pool size if needed
   spring.datasource.hikari.maximum-pool-size=20
   ```

2. **Verify Redis Performance:**
   ```bash
   # Check Redis latency
   redis-cli --latency

   # Monitor cache hit rates
   curl http://localhost:8080/actuator/metrics | grep redis
   ```

3. **Profile Application:**
   ```bash
   # Enable profiling
   java -agentpath:/path/to/async-profiler.jar=start \
        -jar auction-api.jar

   # Check thread dumps
   jstack <pid> > thread_dump.txt
   ```

### Memory Issues

**Symptoms:**
- OutOfMemoryError in logs
- Frequent garbage collection
- Application restarts

**Possible Causes:**
- Memory leaks in application code
- Large result sets from database queries
- Inefficient caching strategies
- High concurrency without proper thread pooling

**Solutions:**
1. **Monitor JVM Memory:**
   ```bash
   # Check heap usage
   jstat -gc <pid>

   # Enable GC logging
   -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
   ```

2. **Analyze Heap Dumps:**
   ```bash
   # Generate heap dump
   jmap -dump:live,format=b,file=heap.hprof <pid>

   # Analyze with tools like Eclipse MAT
   ```

3. **Optimize Queries:**
   - Use pagination for large result sets
   - Implement proper indexing
   - Avoid N+1 query problems

### Rate Limiting Issues

**Symptoms:**
- Legitimate users getting 429 errors
- Inconsistent rate limit enforcement
- Performance degradation under load

**Possible Causes:**
- Redis rate limiter not configured properly
- Clock drift between servers
- Shared Redis instance overloaded

**Solutions:**
1. **Verify Redis Configuration:**
   ```yaml
   auction:
     rate-limit:
       redis:
         host: ${REDIS_HOST}
         port: ${REDIS_PORT}
         timeout: 2000ms
   ```

2. **Check Clock Synchronization:**
   ```bash
   # Verify NTP sync
   ntpq -p
   ```

3. **Monitor Rate Limit Metrics:**
   ```bash
   curl http://localhost:8080/actuator/metrics | grep rate_limit
   ```

## Database Issues

### Connection Pool Exhausted

**Symptoms:**
- "Connection pool exhausted" errors
- Slow database response times
- Application hanging

**Possible Causes:**
- High concurrent load
- Long-running queries
- Connection leaks
- Insufficient pool configuration

**Solutions:**
1. **Increase Pool Size:**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 50
         minimum-idle: 10
   ```

2. **Identify Long-Running Queries:**
   ```sql
   SELECT pid, now() - pg_stat_activity.query_start AS duration,
          query
   FROM pg_stat_activity
   WHERE state = 'active'
   ORDER BY duration DESC;
   ```

3. **Check for Connection Leaks:**
   - Enable connection leak detection
   - Monitor active connections
   - Implement proper connection closing

### Slow Queries

**Symptoms:**
- Database CPU high
- Query timeouts
- Application performance degradation

**Possible Causes:**
- Missing indexes
- Inefficient query patterns
- Large table scans
- Lock contention

**Solutions:**
1. **Analyze Query Performance:**
   ```sql
   EXPLAIN ANALYZE SELECT * FROM bids WHERE auction_id = $1;
   ```

2. **Add Missing Indexes:**
   ```sql
   CREATE INDEX idx_bids_auction_amount ON bids(auction_id, amount DESC);
   CREATE INDEX idx_auctions_end_ts ON auctions(end_ts) WHERE status = 'OPEN';
   ```

3. **Optimize Query Patterns:**
   - Use prepared statements
   - Implement query result caching
   - Consider read replicas for reporting queries

### Replication Lag

**Symptoms:**
- Stale data in read queries
- Inconsistent auction states
- Performance issues on read replicas

**Possible Causes:**
- High write load on primary
- Network issues between primary and replicas
- Replica configuration issues

**Solutions:**
1. **Monitor Replication Lag:**
   ```sql
   SELECT client_addr, state, sent_lsn, write_lsn, flush_lsn, replay_lsn
   FROM pg_stat_replication;
   ```

2. **Optimize Replication:**
   ```postgresql
   # Increase wal_sender timeout
   wal_sender_timeout = 60s

   # Adjust replication slots
   max_replication_slots = 10
   ```

3. **Load Balancing:**
   - Route read queries to replicas
   - Implement connection pooling with replica awareness

## Redis Issues

### Cache Misses

**Symptoms:**
- High database load
- Slow response times
- Increased memory usage

**Possible Causes:**
- Cache TTL too short
- Inefficient cache keys
- Cache invalidation issues
- Redis memory pressure

**Solutions:**
1. **Monitor Cache Statistics:**
   ```bash
   redis-cli info stats | grep keyspace
   ```

2. **Adjust TTL Values:**
   ```yaml
   auction:
     cache:
       ttl:
         auction: 600s  # Increase from 300s
         bid: 7200s     # Increase from 3600s
   ```

3. **Implement Cache Warming:**
   - Pre-populate frequently accessed data
   - Use cache-aside pattern with optimistic updates

### Connection Issues

**Symptoms:**
- Redis connection errors
- Application startup failures
- Intermittent cache misses

**Possible Causes:**
- Network connectivity issues
- Redis server overload
- Authentication failures
- Connection pool exhaustion

**Solutions:**
1. **Test Connectivity:**
   ```bash
   redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping
   ```

2. **Configure Connection Pool:**
   ```yaml
   spring:
     redis:
       lettuce:
         pool:
           max-active: 20
           max-idle: 10
           min-idle: 5
   ```

3. **Enable Redis Sentinel:**
   ```yaml
   spring:
     redis:
       sentinel:
         master: mymaster
         nodes: redis-sentinel:26379
   ```

## Kafka Issues

### Consumer Lag

**Symptoms:**
- Delayed event processing
- Stale notifications
- Analytics data lag

**Possible Causes:**
- Under-provisioned consumer instances
- High message volume
- Processing bottlenecks
- Partition imbalances

**Solutions:**
1. **Monitor Consumer Lag:**
   ```bash
   kafka-consumer-groups --bootstrap-server ${KAFKA_SERVERS} \
                        --group auction-consumers \
                        --describe
   ```

2. **Scale Consumer Groups:**
   ```yaml
   spring:
     kafka:
       consumer:
         concurrency: 5  # Increase from default
   ```

3. **Optimize Processing:**
   - Implement batch processing
   - Use asynchronous processing
   - Monitor processing metrics

### Message Loss

**Symptoms:**
- Missing auction events
- Inconsistent state
- Audit trail gaps

**Possible Causes:**
- Producer configuration issues
- Broker failures
- Network partitions
- Insufficient replication

**Solutions:**
1. **Verify Producer Configuration:**
   ```yaml
   spring:
     kafka:
       producer:
         acks: all
         retries: 10
         delivery.timeout.ms: 30000
   ```

2. **Check Topic Configuration:**
   ```bash
   kafka-topics --bootstrap-server ${KAFKA_SERVERS} \
                --topic auction-events \
                --describe
   ```

3. **Implement Idempotent Producers:**
   ```yaml
   spring:
     kafka:
       producer:
         enable.idempotence: true
   ```

## Timer Issues

### Auction Close Delays

**Symptoms:**
- Auctions not closing on time
- Anti-sniping not working
- Timer drift observed

**Possible Causes:**
- Timer wheel overflow
- Clock synchronization issues
- Persistence failures
- High timer load

**Solutions:**
1. **Monitor Timer Metrics:**
   ```bash
   curl http://localhost:8080/actuator/metrics | grep timer
   ```

2. **Adjust Timer Configuration:**
   ```yaml
   auction:
     timer:
       tick-duration: 50ms  # Decrease for higher precision
       wheel-size: 1024     # Increase for more timers
   ```

3. **Verify Clock Sync:**
   ```bash
   chronyc tracking
   ```

### Timer Persistence Issues

**Symptoms:**
- Timers lost on restart
- Scheduled jobs not recovered
- Inconsistent timer state

**Possible Causes:**
- Database connectivity issues
- Transaction failures
- Concurrency conflicts

**Solutions:**
1. **Check Persistence Logs:**
   ```bash
   grep "timer.*persist" application.log
   ```

2. **Verify Database Transactions:**
   ```sql
   SELECT * FROM scheduled_jobs WHERE status = 'PENDING';
   ```

3. **Implement Reconciliation:**
   - Add startup reconciliation logic
   - Implement timer state validation
   - Add monitoring for timer drift

## Networking Issues

### Load Balancer Problems

**Symptoms:**
- Uneven load distribution
- Sticky session issues
- WebSocket connection failures

**Possible Causes:**
- Incorrect load balancer configuration
- Health check failures
- Session affinity issues

**Solutions:**
1. **Configure Health Checks:**
   ```yaml
   # For Kubernetes ingress
   apiVersion: networking.k8s.io/v1
   kind: Ingress
   metadata:
     annotations:
       nginx.ingress.kubernetes.io/health-check-path: /actuator/health
   ```

2. **WebSocket Support:**
   ```yaml
   # Traefik configuration
   apiVersion: traefik.containo.us/v1alpha1
   kind: IngressRoute
   metadata:
     name: auction-api
   spec:
     routes:
     - match: Host(`api.auctionflow.com`)
       kind: Rule
       services:
       - name: auction-api
         port: 8080
     - match: Host(`api.auctionflow.com`) && Path(`/ws`)
       kind: Rule
       services:
       - name: auction-api
         port: 8080
   ```

## Monitoring and Alerting

### Key Metrics to Monitor

**Application Metrics:**
- Bid latency percentiles (P50, P95, P99)
- Error rates by endpoint
- JVM memory and GC metrics
- Thread pool utilization

**Database Metrics:**
- Connection pool usage
- Query execution times
- Lock wait times
- Replication lag

**Infrastructure Metrics:**
- CPU and memory usage
- Disk I/O and network I/O
- Redis memory usage
- Kafka broker metrics

### Alerting Rules

```yaml
# Prometheus alerting rules
groups:
- name: auction_flow_alerts
  rules:
  - alert: HighBidLatency
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket{job="auction-api"}[5m])) > 0.1
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "High bid latency detected"

  - alert: DatabaseConnectionPoolExhausted
    expr: jdbc_connections_active / jdbc_connections_max > 0.9
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "Database connection pool nearly exhausted"
```

## Getting Help

### Log Analysis
```bash
# Search for errors
grep "ERROR" application.log | tail -20

# Analyze stack traces
grep -A 10 "Exception" application.log
```

### Diagnostic Commands
```bash
# System diagnostics
top -p <pid>
iostat -x 1
netstat -tlnp

# Application diagnostics
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

### Support Resources
- **Documentation**: Check API reference and deployment guide
- **Community**: GitHub issues and discussions
- **Professional Support**: Contact enterprise support team
- **Monitoring Dashboards**: Grafana dashboards for real-time monitoring

## Prevention

### Best Practices
1. **Implement Comprehensive Monitoring**
2. **Set Up Proper Alerting**
3. **Regular Performance Testing**
4. **Automated Health Checks**
5. **Capacity Planning**
6. **Regular Backups and Testing**

### Proactive Measures
- Load testing before deployments
- Chaos engineering exercises
- Regular dependency updates
- Security audits and penetration testing