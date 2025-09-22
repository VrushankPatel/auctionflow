#!/bin/bash

# Redis Backup Script for Auction Flow
# This script creates RDB snapshots of Redis data

set -e

# Configuration
BACKUP_DIR="./backups/redis"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/redis_backup_${TIMESTAMP}.rdb.gz"
LOG_FILE="${BACKUP_DIR}/backup_log.txt"

# Redis connection details
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-7001}  # First node in cluster
REDIS_PASSWORD=${REDIS_PASSWORD:-}

# Retention: keep last 7 daily backups
RETENTION_DAILY=7

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Function to log messages
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" | tee -a "$LOG_FILE"
}

# Function to check Redis connectivity
check_redis() {
    if ! redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping >/dev/null 2>&1; then
        log "ERROR: Cannot connect to Redis at $REDIS_HOST:$REDIS_PORT"
        exit 1
    fi
}

# Function to trigger BGSAVE
trigger_bgsave() {
    log "Triggering BGSAVE on Redis..."

    local result
    result=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" BGSAVE)

    if [[ "$result" == "Background saving started" ]]; then
        log "BGSAVE started successfully"
    else
        log "ERROR: BGSAVE failed: $result"
        exit 1
    fi
}

# Function to wait for BGSAVE completion
wait_for_bgsave() {
    log "Waiting for BGSAVE to complete..."

    local timeout=300  # 5 minutes timeout
    local elapsed=0

    while [[ $elapsed -lt $timeout ]]; do
        local info
        info=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO persistence)

        if echo "$info" | grep -q "rdb_bgsave_in_progress:0"; then
            log "BGSAVE completed successfully"
            return 0
        fi

        sleep 5
        elapsed=$((elapsed + 5))
    done

    log "ERROR: BGSAVE did not complete within $timeout seconds"
    exit 1
}

# Function to copy RDB file
copy_rdb_file() {
    log "Copying RDB file..."

    # For Redis cluster, we need to backup from each node
    # This is a simplified version - in production, you'd backup all nodes

    # Assuming the RDB file is in the default location inside container
    # In docker-compose, data is in named volumes, so we need to copy from container

    local container_name="redis-1"  # First Redis node

    if docker ps | grep -q "$container_name"; then
        docker exec "$container_name" sh -c "if [ -f /data/dump.rdb ]; then cp /data/dump.rdb /data/dump_${TIMESTAMP}.rdb; fi"

        # Copy from container to host
        docker cp "$container_name:/data/dump_${TIMESTAMP}.rdb" "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb" 2>/dev/null || {
            log "WARNING: Could not copy RDB from container, checking if AOF backup is available"
            # Fallback: since AOF is enabled, we can use that as backup
            docker exec "$container_name" sh -c "if [ -f /data/appendonly.aof ]; then cp /data/appendonly.aof /data/appendonly_${TIMESTAMP}.aof; fi"
            docker cp "$container_name:/data/appendonly_${TIMESTAMP}.aof" "${BACKUP_DIR}/appendonly_${TIMESTAMP}.aof" 2>/dev/null || {
                log "ERROR: No backup files found in Redis container"
                exit 1
            }
            BACKUP_FILE="${BACKUP_DIR}/redis_aof_backup_${TIMESTAMP}.aof.gz"
            gzip "${BACKUP_DIR}/appendonly_${TIMESTAMP}.aof"
            mv "${BACKUP_DIR}/appendonly_${TIMESTAMP}.aof.gz" "$BACKUP_FILE"
            return
        }

        # Compress the RDB file
        gzip "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb"
        mv "${BACKUP_DIR}/dump_${TIMESTAMP}.rdb.gz" "$BACKUP_FILE"
    else
        log "ERROR: Redis container $container_name not running"
        exit 1
    fi
}

# Function to clean up old backups
cleanup_old_backups() {
    log "Cleaning up old Redis backups..."

    find "$BACKUP_DIR" -name "redis_backup_*.rdb.gz" -type f -mtime +$RETENTION_DAILY -delete 2>/dev/null || true
    find "$BACKUP_DIR" -name "redis_aof_backup_*.aof.gz" -type f -mtime +$RETENTION_DAILY -delete 2>/dev/null || true

    log "Cleanup completed"
}

# Main backup function
perform_backup() {
    log "Starting Redis backup..."

    check_redis

    # For Redis cluster, we might need to handle multiple nodes
    # This is simplified for the first node

    # Trigger BGSAVE if not already in progress
    local bgsave_status
    bgsave_status=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" INFO persistence | grep rdb_bgsave_in_progress | cut -d: -f2)

    if [[ "$bgsave_status" != "1" ]]; then
        trigger_bgsave
        wait_for_bgsave
    else
        log "BGSAVE already in progress, waiting for completion..."
        wait_for_bgsave
    fi

    # Copy the backup file
    copy_rdb_file

    # Verify backup
    if [[ -f "$BACKUP_FILE" ]]; then
        log "Backup completed successfully: $BACKUP_FILE"
        log "Backup size: $(du -h "$BACKUP_FILE" | cut -f1)"
    else
        log "ERROR: Backup file not created!"
        exit 1
    fi
}

# Function to send notification
send_notification() {
    local message="$1"
    log "Notification: $message"
}

# Main execution
main() {
    log "=== Redis Backup Started ==="

    if perform_backup; then
        send_notification "Redis backup completed successfully"
        cleanup_old_backups
    else
        send_notification "Redis backup failed!"
        exit 1
    fi

    log "=== Redis Backup Completed ==="
}

# Run main function
main "$@"