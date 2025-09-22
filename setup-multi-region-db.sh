#!/bin/bash

# Script to set up Aurora Global Database for multi-region replication
# Assumes AWS CLI configured

PRIMARY_REGION="us-east-1"
SECONDARY_REGIONS=("eu-west-1" "ap-southeast-1")
DB_CLUSTER_IDENTIFIER="auctionflow-global-db"
DB_ENGINE="aurora-postgresql"
DB_ENGINE_VERSION="13.7"
MASTER_USERNAME="auctionuser"
MASTER_PASSWORD="auctionpass"  # Use secrets manager in prod
DB_INSTANCE_CLASS="db.r5.large"
DB_SUBNET_GROUP="auctionflow-subnet-group"  # Assume exists
DB_SECURITY_GROUP="auctionflow-sg"  # Assume exists

# Create primary cluster in primary region
aws rds create-db-cluster \
  --region $PRIMARY_REGION \
  --db-cluster-identifier $DB_CLUSTER_IDENTIFIER \
  --engine $DB_ENGINE \
  --engine-version $DB_ENGINE_VERSION \
  --master-username $MASTER_USERNAME \
  --master-user-password $MASTER_PASSWORD \
  --db-subnet-group-name $DB_SUBNET_GROUP \
  --vpc-security-group-ids $DB_SECURITY_GROUP \
  --enable-global-write-forwarding \
  --enable-http-endpoint

# Add secondary regions
for REGION in "${SECONDARY_REGIONS[@]}"; do
  aws rds create-db-cluster \
    --region $REGION \
    --db-cluster-identifier $DB_CLUSTER_IDENTIFIER \
    --engine $DB_ENGINE \
    --engine-version $DB_ENGINE_VERSION \
    --global-cluster-identifier $DB_CLUSTER_IDENTIFIER \
    --db-subnet-group-name $DB_SUBNET_GROUP \
    --vpc-security-group-ids $DB_SECURITY_GROUP \
    --enable-global-write-forwarding
done

# Create DB instances in each region
for REGION in $PRIMARY_REGION "${SECONDARY_REGIONS[@]}"; do
  aws rds create-db-instance \
    --region $REGION \
    --db-instance-identifier "${DB_CLUSTER_IDENTIFIER}-instance" \
    --db-instance-class $DB_INSTANCE_CLASS \
    --db-cluster-identifier $DB_CLUSTER_IDENTIFIER \
    --engine $DB_ENGINE
done

echo "Aurora Global Database setup complete."