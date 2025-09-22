#!/bin/bash

# Automated PostgreSQL Backup Script for Auction Flow
# This script performs full database backups using pg_dump

set -e

# Configuration
BACKUP_DIR="./backups/postgres"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/auction_flow_backup_${TIMESTAMP}.sql.gz"
LOG_FILE="${BACKUP_DIR}/backup_log.txt"

# Database connection details (from environment or defaults)
DB_HOST=${POSTGRES_HOST:-localhost}
DB_PORT=${POSTGRES_PORT:-5432}
DB_NAME=${POSTGRES_DB:-auctionflow}
DB_USER=${POSTGRES_USER:-auctionuser}
DB_PASSWORD=${POSTGRES_PASSWORD:-auctionpass}

# Retention: keep last 7 daily backups, 4 weekly, 12 monthly
RETENTION_DAILY=7
RETENTION_WEEKLY=4
RETENTION_MONTHLY=12

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Function to log messages
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" | tee -a "$LOG_FILE"
}

# Function to clean up old backups
cleanup_old_backups() {
    log "Cleaning up old backups..."

    # Daily backups (keep last 7)
    find "$BACKUP_DIR" -name "auction_flow_backup_*.sql.gz" -type f -mtime +$RETENTION_DAILY -delete 2>/dev/null || true

    # Weekly backups (Sundays, keep last 4)
    find "$BACKUP_DIR" -name "auction_flow_backup_*_23.sql.gz" -type f -mtime +$(($RETENTION_WEEKLY * 7)) -delete 2>/dev/null || true

    # Monthly backups (1st of month, keep last 12)
    find "$BACKUP_DIR" -name "auction_flow_backup_*01_*.sql.gz" -type f -mtime +$(($RETENTION_MONTHLY * 30)) -delete 2>/dev/null || true

    log "Cleanup completed"
}

# Main backup function
perform_backup() {
    log "Starting PostgreSQL backup..."

    # Export password for pg_dump
    export PGPASSWORD="$DB_PASSWORD"

    # Perform the backup
    pg_dump \
        --host="$DB_HOST" \
        --port="$DB_PORT" \
        --username="$DB_USER" \
        --dbname="$DB_NAME" \
        --no-password \
        --format=custom \
        --compress=9 \
        --verbose \
        --file="${BACKUP_DIR}/auction_flow_backup_${TIMESTAMP}.backup"

    # Compress the backup
    gzip "${BACKUP_DIR}/auction_flow_backup_${TIMESTAMP}.backup"
    mv "${BACKUP_DIR}/auction_flow_backup_${TIMESTAMP}.backup.gz" "$BACKUP_FILE"

    # Verify backup integrity
    if gunzip -c "$BACKUP_FILE" | pg_restore --list > /dev/null 2>&1; then
        log "Backup completed successfully: $BACKUP_FILE"
        log "Backup size: $(du -h "$BACKUP_FILE" | cut -f1)"
    else
        log "ERROR: Backup verification failed!"
        exit 1
    fi

    # Unset password
    unset PGPASSWORD
}

# Function to send notification (placeholder for future integration)
send_notification() {
    local message="$1"
    # TODO: Integrate with notification system (email, Slack, etc.)
    log "Notification: $message"
}

# Main execution
main() {
    log "=== PostgreSQL Backup Started ==="

    # Perform backup
    if perform_backup; then
        send_notification "PostgreSQL backup completed successfully"
        cleanup_old_backups
    else
        send_notification "PostgreSQL backup failed!"
        exit 1
    fi

    log "=== PostgreSQL Backup Completed ==="
}

# Run main function
main "$@"