#!/bin/bash

# Setup Cron Jobs for Automated Backups
# This script sets up cron jobs for regular backup execution

set -e

# Configuration
CRON_USER=${CRON_USER:-$(whoami)}
BACKUP_SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Function to log messages
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1"
}

# Function to add cron job
add_cron_job() {
    local schedule="$1"
    local script_path="$2"
    local job_name="$3"

    # Check if job already exists
    if crontab -l 2>/dev/null | grep -q "$script_path"; then
        log "Cron job for $job_name already exists"
        return
    fi

    # Add the job
    (crontab -l 2>/dev/null; echo "$schedule $script_path") | crontab -

    log "Added cron job for $job_name: $schedule $script_path"
}

# Function to setup backup cron jobs
setup_backup_crons() {
    log "Setting up backup cron jobs..."

    # PostgreSQL backup: daily at 2 AM
    add_cron_job "0 2 * * *" "$BACKUP_SCRIPTS_DIR/postgres_backup.sh" "PostgreSQL Backup"

    # Redis backup: daily at 3 AM
    add_cron_job "0 3 * * *" "$BACKUP_SCRIPTS_DIR/redis_backup.sh" "Redis Backup"

    # Kafka backup: daily at 4 AM
    add_cron_job "0 4 * * *" "$BACKUP_SCRIPTS_DIR/kafka_backup.sh" "Kafka Backup"

    # Backup testing: weekly on Sundays at 5 AM
    add_cron_job "0 5 * * 0" "$BACKUP_SCRIPTS_DIR/test_backup_restore.sh" "Backup Testing"

    log "Backup cron jobs setup completed"
}

# Function to list current cron jobs
list_cron_jobs() {
    log "Current cron jobs for $CRON_USER:"
    crontab -l
}

# Function to remove backup cron jobs
remove_backup_crons() {
    log "Removing backup cron jobs..."

    # Create a temporary file with jobs to keep
    crontab -l 2>/dev/null | grep -v "postgres_backup.sh\|redis_backup.sh\|kafka_backup.sh\|test_backup_restore.sh" > /tmp/cron_jobs.tmp || true

    # Install the filtered cron jobs
    crontab /tmp/cron_jobs.tmp
    rm -f /tmp/cron_jobs.tmp

    log "Backup cron jobs removed"
}

# Main execution
main() {
    case "${1:-setup}" in
        setup)
            setup_backup_crons
            list_cron_jobs
            ;;
        list)
            list_cron_jobs
            ;;
        remove)
            remove_backup_crons
            list_cron_jobs
            ;;
        *)
            echo "Usage: $0 {setup|list|remove}"
            echo "  setup  - Setup backup cron jobs"
            echo "  list   - List current cron jobs"
            echo "  remove - Remove backup cron jobs"
            exit 1
            ;;
    esac
}

# Run main function
main "$@"