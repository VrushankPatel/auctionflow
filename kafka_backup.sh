#!/bin/bash

# Kafka Event Store Backup Script for Auction Flow
# This script creates snapshots of Kafka topics for backup purposes

set -e

# Configuration
BACKUP_DIR="./backups/kafka"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/kafka_topics_backup_${TIMESTAMP}.tar.gz"
LOG_FILE="${BACKUP_DIR}/backup_log.txt"

# Kafka connection details
KAFKA_BOOTSTRAP_SERVERS=${KAFKA_BOOTSTRAP_SERVERS:-localhost:9092}
KAFKA_TOPICS=("auction-events" "bid-events" "auction-closed" "payment-events")  # Add relevant topics

# Retention: keep last 7 daily backups
RETENTION_DAILY=7

# Create backup directory if it doesn't exist
mkdir -p "$BACKUP_DIR"

# Function to log messages
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" | tee -a "$LOG_FILE"
}

# Function to check if kafka-console-consumer is available
check_kafka_tools() {
    if ! command -v kafka-console-consumer.sh >/dev/null 2>&1; then
        log "ERROR: kafka-console-consumer.sh not found. Please ensure Kafka tools are installed."
        exit 1
    fi
}

# Function to backup a single topic
backup_topic() {
    local topic="$1"
    local topic_backup_dir="${BACKUP_DIR}/topics_${TIMESTAMP}/${topic}"

    mkdir -p "$topic_backup_dir"

    log "Backing up topic: $topic"

    # Export topic data to JSON files
    # Note: This is a simplified approach. For large topics, consider using Kafka Connect or MirrorMaker
    kafka-console-consumer.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
        --topic "$topic" \
        --from-beginning \
        --timeout-ms 60000 \
        --property "print.key=true" \
        --property "key.separator=|" \
        > "${topic_backup_dir}/data.json" 2>>"$LOG_FILE"

    # Get topic configuration
    kafka-topics.sh \
        --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
        --topic "$topic" \
        --describe \
        > "${topic_backup_dir}/config.txt" 2>>"$LOG_FILE"

    log "Topic $topic backed up successfully"
}

# Function to clean up old backups
cleanup_old_backups() {
    log "Cleaning up old Kafka backups..."

    find "$BACKUP_DIR" -name "kafka_topics_backup_*.tar.gz" -type f -mtime +$RETENTION_DAILY -delete 2>/dev/null || true

    log "Cleanup completed"
}

# Main backup function
perform_backup() {
    log "Starting Kafka topics backup..."

    check_kafka_tools

    local temp_dir="${BACKUP_DIR}/topics_${TIMESTAMP}"
    mkdir -p "$temp_dir"

    # Backup each topic
    for topic in "${KAFKA_TOPICS[@]}"; do
        if backup_topic "$topic"; then
            log "Successfully backed up topic: $topic"
        else
            log "WARNING: Failed to backup topic: $topic"
        fi
    done

    # Compress the backup
    log "Compressing backup..."
    tar -czf "$BACKUP_FILE" -C "$BACKUP_DIR" "topics_${TIMESTAMP}"

    # Verify backup
    if tar -tzf "$BACKUP_FILE" >/dev/null 2>&1; then
        log "Backup completed successfully: $BACKUP_FILE"
        log "Backup size: $(du -h "$BACKUP_FILE" | cut -f1)"

        # Clean up temp directory
        rm -rf "$temp_dir"
    else
        log "ERROR: Backup verification failed!"
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
    log "=== Kafka Backup Started ==="

    if perform_backup; then
        send_notification "Kafka topics backup completed successfully"
        cleanup_old_backups
    else
        send_notification "Kafka topics backup failed!"
        exit 1
    fi

    log "=== Kafka Backup Completed ==="
}

# Run main function
main "$@"