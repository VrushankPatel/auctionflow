#!/bin/bash

# Backup and Restore Testing Automation for Auction Flow
# This script tests the integrity of backups by attempting to restore them

set -e

# Configuration
BACKUP_DIR="./backups"
LOG_FILE="./backups/test_log.txt"
TEST_DB_NAME="auctionflow_test"
TEST_RESTORE_DIR="./backups/test_restore"

# Database connection details
DB_HOST=${POSTGRES_HOST:-localhost}
DB_PORT=${POSTGRES_PORT:-5432}
DB_USER=${POSTGRES_USER:-auctionuser}
DB_PASSWORD=${POSTGRES_PASSWORD:-auctionpass}

# Function to log messages
log() {
    echo "$(date +"%Y-%m-%d %H:%M:%S") - $1" | tee -a "$LOG_FILE"
}

# Function to setup test environment
setup_test_env() {
    log "Setting up test environment..."

    mkdir -p "$TEST_RESTORE_DIR"

    # Create test database if it doesn't exist
    export PGPASSWORD="$DB_PASSWORD"
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE IF NOT EXISTS $TEST_DB_NAME;" 2>/dev/null || true
    unset PGPASSWORD
}

# Function to test PostgreSQL backup restore
test_postgres_restore() {
    log "Testing PostgreSQL backup restore..."

    # Find the most recent backup
    local latest_backup=$(find "$BACKUP_DIR/postgres" -name "auction_flow_backup_*.sql.gz" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)

    if [[ -z "$latest_backup" ]]; then
        log "WARNING: No PostgreSQL backup found, skipping test"
        return 1
    fi

    log "Testing restore from: $latest_backup"

    # Create a temporary database for testing
    local test_db="${TEST_DB_NAME}_restore_test"

    export PGPASSWORD="$DB_PASSWORD"

    # Drop test database if exists
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $test_db;" 2>/dev/null || true

    # Create test database
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $test_db;"

    # Restore from backup
    gunzip -c "$latest_backup" | psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$test_db" > /dev/null 2>&1

    # Verify restore by checking if tables exist
    local table_count
    table_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$test_db" -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" 2>/dev/null || echo "0")

    if [[ $table_count -gt 0 ]]; then
        log "PostgreSQL restore test PASSED"
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE $test_db;"
        unset PGPASSWORD
        return 0
    else
        log "PostgreSQL restore test FAILED"
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE $test_db;" 2>/dev/null || true
        unset PGPASSWORD
        return 1
    fi
}

# Function to test Redis backup restore
test_redis_restore() {
    log "Testing Redis backup restore..."

    # Find the most recent backup
    local latest_backup=$(find "$BACKUP_DIR/redis" -name "redis_backup_*.rdb.gz" -o -name "redis_aof_backup_*.aof.gz" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)

    if [[ -z "$latest_backup" ]]; then
        log "WARNING: No Redis backup found, skipping test"
        return 1
    fi

    log "Testing restore from: $latest_backup"

    # For Redis testing, we can check if the file is valid
    # For RDB files
    if [[ "$latest_backup" == *.rdb.gz ]]; then
        if gunzip -c "$latest_backup" | head -c 9 | grep -q "REDIS"; then
            log "Redis RDB restore test PASSED"
            return 0
        else
            log "Redis RDB restore test FAILED - invalid RDB file"
            return 1
        fi
    # For AOF files
    elif [[ "$latest_backup" == *.aof.gz ]]; then
        if gunzip -c "$latest_backup" | head -n 1 | grep -q "*"; then
            log "Redis AOF restore test PASSED"
            return 0
        else
            log "Redis AOF restore test FAILED - invalid AOF file"
            return 1
        fi
    else
        log "Redis restore test FAILED - unknown backup format"
        return 1
    fi
}

# Function to test Kafka backup restore
test_kafka_restore() {
    log "Testing Kafka backup restore..."

    # Find the most recent backup
    local latest_backup=$(find "$BACKUP_DIR/kafka" -name "kafka_topics_backup_*.tar.gz" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)

    if [[ -z "$latest_backup" ]]; then
        log "WARNING: No Kafka backup found, skipping test"
        return 1
    fi

    log "Testing restore from: $latest_backup"

    # Extract backup to test directory
    local extract_dir="$TEST_RESTORE_DIR/kafka_test"
    mkdir -p "$extract_dir"
    tar -xzf "$latest_backup" -C "$extract_dir"

    # Check if topics directory exists and has data
    if [[ -d "$extract_dir" ]] && find "$extract_dir" -name "*.json" -type f | grep -q .; then
        log "Kafka backup restore test PASSED"
        rm -rf "$extract_dir"
        return 0
    else
        log "Kafka backup restore test FAILED - no valid data found"
        rm -rf "$extract_dir"
        return 1
    fi
}

# Function to send test results
send_test_results() {
    local results="$1"
    log "Test Results: $results"
    # TODO: Send to monitoring system
}

# Function to cleanup test environment
cleanup_test_env() {
    log "Cleaning up test environment..."
    rm -rf "$TEST_RESTORE_DIR"
}

# Main test function
run_tests() {
    log "=== Backup Restore Tests Started ==="

    local passed=0
    local failed=0
    local results=""

    # Test PostgreSQL
    if test_postgres_restore; then
        results="${results}PostgreSQL:PASS "
        ((passed++))
    else
        results="${results}PostgreSQL:FAIL "
        ((failed++))
    fi

    # Test Redis
    if test_redis_restore; then
        results="${results}Redis:PASS "
        ((passed++))
    else
        results="${results}Redis:FAIL "
        ((failed++))
    fi

    # Test Kafka
    if test_kafka_restore; then
        results="${results}Kafka:PASS "
        ((passed++))
    else
        results="${results}Kafka:FAIL "
        ((failed++))
    fi

    log "Tests completed: $passed passed, $failed failed"

    send_test_results "$results"

    if [[ $failed -gt 0 ]]; then
        log "=== Some tests FAILED ==="
        return 1
    else
        log "=== All tests PASSED ==="
        return 0
    fi
}

# Main execution
main() {
    setup_test_env

    if run_tests; then
        cleanup_test_env
        exit 0
    else
        cleanup_test_env
        exit 1
    fi
}

# Run main function
main "$@"