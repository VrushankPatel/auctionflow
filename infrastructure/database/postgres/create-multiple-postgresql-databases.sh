#!/bin/bash

set -e
set -u

function create_user_and_database() {
    local database=$1
    echo "  Creating user and database '$database'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE $database'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$database')\gexec
        
        DO \$\$
        BEGIN
            IF NOT EXISTS (
                SELECT FROM pg_roles WHERE rolname = '$database'
            ) THEN
                CREATE USER $database WITH PASSWORD '$POSTGRES_PASSWORD';
            END IF;
        END
        \$\$;

        GRANT ALL PRIVILEGES ON DATABASE $database TO $database;
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO $database;
EOSQL
}

if [ -n "$POSTGRES_MULTIPLE_DATABASES" ]; then
    echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
    for db in $(echo $POSTGRES_MULTIPLE_DATABASES | tr ',' ' '); do
        create_user_and_database $db
    done
    echo "Multiple databases created"
fi