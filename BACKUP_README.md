# Backup and Recovery System for Auction Flow

This document describes the backup and recovery implementation for the Auction Flow system.

## Overview

The backup system provides automated backups for all critical components:
- **PostgreSQL**: Automated daily backups with Point-in-Time Recovery (PITR) capability
- **Redis**: RDB snapshots with AOF persistence
- **Kafka**: Event store topic exports for disaster recovery

## Components

### PostgreSQL Backups

**Automated Backups** (`postgres_backup.sh`):
- Daily full backups using `pg_dump` with custom format
- Compressed with gzip for storage efficiency
- Retention policy: 7 daily, 4 weekly, 12 monthly backups
- Verification of backup integrity

**Point-in-Time Recovery** (`postgres_pitr_restore.sh`):
- Restore database to any specific timestamp
- Uses WAL archives for precise recovery
- Requires base backup + WAL files

**Configuration**:
- WAL archiving enabled in `postgres/postgresql-primary.conf`
- Archive directory mounted as Docker volume

### Redis Backups

**Automated Backups** (`redis_backup.sh`):
- Daily RDB snapshots using BGSAVE
- Fallback to AOF file if RDB unavailable
- Compressed storage
- Retention: 7 daily backups

**Persistence**:
- AOF (Append Only File) enabled for durability
- Configured in docker-compose.yml

### Kafka Backups

**Event Store Snapshots** (`kafka_backup.sh`):
- Export of critical topics to JSON format
- Topic configuration backup
- Compressed tar.gz archives
- Retention: 7 daily backups

**Topics Backed Up**:
- auction-events
- bid-events
- auction-closed
- payment-events

### Testing Automation

**Backup Verification** (`test_backup_restore.sh`):
- Automated testing of backup integrity
- Attempts restore operations in test environment
- Validates backup files are restorable
- Weekly execution via cron

## Setup Instructions

### 1. Environment Variables

Ensure the following environment variables are set in your `.env` file:

```bash
POSTGRES_DB=auctionflow
POSTGRES_USER=auctionuser
POSTGRES_PASSWORD=your_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

REDIS_HOST=localhost
REDIS_PORT=7001

KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### 2. Directory Structure

Create the backup directories:

```bash
mkdir -p backups/postgres
mkdir -p backups/redis
mkdir -p backups/kafka
```

### 3. Docker Compose

The `docker-compose.yml` has been updated with:
- PostgreSQL WAL archiving configuration
- Archive volume mount
- Redis AOF persistence

### 4. Cron Jobs

Set up automated backups using the cron setup script:

```bash
# Setup cron jobs for automated backups
./setup_backup_cron.sh setup

# List current cron jobs
./setup_backup_cron.sh list

# Remove cron jobs
./setup_backup_cron.sh remove
```

**Cron Schedule**:
- PostgreSQL: Daily at 2:00 AM
- Redis: Daily at 3:00 AM
- Kafka: Daily at 4:00 AM
- Testing: Weekly on Sundays at 5:00 AM

## Manual Operations

### Running Backups Manually

```bash
# PostgreSQL backup
./postgres_backup.sh

# Redis backup
./redis_backup.sh

# Kafka backup
./kafka_backup.sh

# Test all backups
./test_backup_restore.sh
```

### Point-in-Time Recovery

```bash
# Restore to specific timestamp
./postgres_pitr_restore.sh "2024-01-15 14:30:00"
```

## Monitoring and Alerting

- All scripts log to respective `backup_log.txt` files
- Test results are logged for monitoring
- TODO: Integrate with notification systems (email, Slack, etc.)

## Recovery Procedures

### PostgreSQL Recovery

1. Stop the application services
2. Choose recovery point (latest backup or specific timestamp)
3. For full restore: Use `pg_restore` from backup file
4. For PITR: Use `postgres_pitr_restore.sh` script
5. Start PostgreSQL and verify data integrity
6. Restart application services

### Redis Recovery

1. Stop Redis cluster
2. Replace data files with backup
3. Start Redis cluster
4. Verify data consistency

### Kafka Recovery

1. Stop Kafka cluster
2. Recreate topics with saved configurations
3. Import data from backup files
4. Start Kafka cluster
5. Verify event stream integrity

## Security Considerations

- Backup files contain sensitive data
- Store backups in encrypted storage
- Restrict access to backup scripts and files
- Use secure transfer methods for offsite backups

## Performance Impact

- Backups are scheduled during low-traffic hours
- PostgreSQL uses `pg_dump` which locks tables briefly
- Redis BGSAVE runs in background
- Kafka exports may impact broker performance

## Future Enhancements

- Offsite backup storage (S3, GCS, etc.)
- Encrypted backups
- Backup compression optimization
- Integration with monitoring systems
- Automated failover testing