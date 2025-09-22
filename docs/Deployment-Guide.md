# Auction Flow Deployment Guide

## Prerequisites

### System Requirements
- **Java**: JDK 17 or later
- **Build Tool**: Gradle 7.0+
- **Database**: PostgreSQL 13+
- **Cache**: Redis 6.0+
- **Message Queue**: Apache Kafka 2.8+
- **Container Runtime**: Docker 20.10+ (optional)
- **Kubernetes**: 1.20+ (for production deployment)

### Hardware Requirements
- **Development**: 4 CPU cores, 8GB RAM, 50GB storage
- **Production**: 8+ CPU cores, 16GB+ RAM, 100GB+ SSD storage
- **High Availability**: Multiple nodes with load balancer

## Local Development Setup

### 1. Clone Repository
```bash
git clone https://github.com/your-org/auction-flow.git
cd auction-flow
```

### 2. Start Dependencies with Docker Compose
```bash
docker-compose up -d postgres redis kafka
```

### 3. Configure Environment
Create `.env` file:
```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=auctionflow
DB_USER=auctionflow
DB_PASSWORD=password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET=your-secret-key-here

# Payment Provider
PAYMENT_API_KEY=your-payment-api-key
```

### 4. Initialize Database
```bash
./gradlew flywayMigrate
```

### 5. Build and Run
```bash
./gradlew build
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

## Production Deployment

### Docker Deployment

#### Build Docker Images
```bash
# Build all modules
./gradlew bootBuildImage

# Or build specific service
cd auction-api
./gradlew bootBuildImage
```

#### Run with Docker Compose
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Kubernetes Deployment

#### Using Helm Charts
```bash
# Install Auction Flow
helm install auction-flow ./helm/auction-api

# Install all services
helm install auction-analytics ./helm/auction-analytics
helm install auction-events ./helm/auction-events
# ... install other services
```

#### Manual Kubernetes Deployment
```bash
# Apply configurations
kubectl apply -f k8s/

# Check status
kubectl get pods
kubectl get services
```

### Cloud Deployment Options

#### AWS
- **ECS/EKS**: Use provided Helm charts
- **RDS**: PostgreSQL managed database
- **ElastiCache**: Redis cluster
- **MSK**: Managed Streaming for Kafka

#### GCP
- **GKE**: Kubernetes deployment
- **Cloud SQL**: PostgreSQL
- **Memorystore**: Redis
- **Pub/Sub**: Alternative to Kafka

#### Azure
- **AKS**: Kubernetes service
- **Database for PostgreSQL**: Managed database
- **Cache for Redis**: Managed cache
- **Event Hubs**: Alternative messaging

## Configuration

### Application Properties

Key configuration properties in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}

kafka:
  bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}

auction:
  timer:
    tick-duration: 100ms
    wheel-size: 512

  rate-limit:
    bids-per-second: 5
    burst-limit: 20

  cache:
    ttl:
      auction: 300s
      bid: 3600s
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | auctionflow |
| `DB_USER` | Database user | auctionflow |
| `DB_PASSWORD` | Database password | - |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | localhost:9092 |
| `JWT_SECRET` | JWT signing secret | - |
| `PAYMENT_API_KEY` | Payment provider API key | - |

## Database Setup

### PostgreSQL Configuration
```sql
-- Create database
CREATE DATABASE auctionflow;

-- Create user
CREATE USER auctionflow WITH PASSWORD 'password';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE auctionflow TO auctionflow;

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### Schema Migration
```bash
# Run migrations
./gradlew flywayMigrate

# Clean and migrate (development only)
./gradlew flywayClean flywayMigrate
```

## Monitoring and Observability

### Health Checks
- **Application**: `GET /actuator/health`
- **Database**: `GET /actuator/health/db`
- **Redis**: `GET /actuator/health/redis`
- **Kafka**: `GET /actuator/health/kafka`

### Metrics
- **Prometheus**: `GET /actuator/prometheus`
- **Custom Metrics**: Bid latency, throughput, error rates

### Logging
```yaml
logging:
  level:
    com.auctionflow: INFO
    org.springframework: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
```

## Security Configuration

### TLS/SSL
```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
```

### CORS Configuration
```yaml
auction:
  cors:
    allowed-origins: "https://yourdomain.com"
    allowed-methods: "GET,POST,PUT,DELETE,OPTIONS"
    allowed-headers: "*"
```

## Scaling Considerations

### Horizontal Scaling
- **API Services**: Stateless, scale via Kubernetes HPA
- **Database**: Read replicas for query scaling
- **Redis**: Cluster mode for cache scaling
- **Kafka**: Increase partitions for higher throughput

### Load Balancing
- Use ingress controller (Traefik/Nginx) for API routing
- Configure sticky sessions for WebSocket connections
- Implement rate limiting at load balancer level

## Backup and Recovery

### Database Backup
```bash
# Automated backup script
./postgres_backup.sh

# Point-in-time recovery
./postgres_pitr_restore.sh
```

### Disaster Recovery
- Multi-region deployment with data replication
- Automated failover procedures
- Regular backup testing

## Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Check connectivity
psql -h ${DB_HOST} -U ${DB_USER} -d ${DB_NAME}

# Verify credentials
kubectl get secret postgres-secret -o yaml
```

#### Redis Connection Issues
```bash
# Test Redis connection
redis-cli -h ${REDIS_HOST} -p ${REDIS_PORT} ping
```

#### Kafka Issues
```bash
# Check Kafka topics
kafka-topics --bootstrap-server ${KAFKA_BOOTSTRAP_SERVERS} --list

# Verify consumer groups
kafka-consumer-groups --bootstrap-server ${KAFKA_BOOTSTRAP_SERVERS} --list
```

#### Performance Issues
- Monitor JVM metrics with JMX
- Check database query performance
- Analyze Kafka consumer lag
- Review Redis memory usage

## Maintenance

### Regular Tasks
- **Log Rotation**: Configure log rotation policies
- **Certificate Renewal**: Monitor SSL certificate expiry
- **Dependency Updates**: Regular security updates
- **Performance Tuning**: Monitor and adjust configurations

### Upgrade Procedure
1. Backup current state
2. Update Helm charts or Docker images
3. Run database migrations
4. Deploy new version with zero-downtime strategy
5. Verify functionality with smoke tests
6. Rollback if issues detected

## Support

For deployment issues:
- Check logs: `kubectl logs <pod-name>`
- Review metrics in Grafana/Prometheus
- Consult troubleshooting guide
- Contact DevOps team for complex issues