#!/bin/bash

# Script to set up ElastiCache Global Datastore for Redis
# Assumes AWS CLI configured

PRIMARY_REGION="us-east-1"
SECONDARY_REGIONS=("eu-west-1" "ap-southeast-1")
GLOBAL_DATASTORE_NAME="auctionflow-global-redis"
PRIMARY_CLUSTER_ID="auctionflow-redis-primary"
SUBNET_GROUP="auctionflow-redis-subnet"  # Assume exists
SECURITY_GROUP="auctionflow-redis-sg"  # Assume exists

# Create primary cluster
aws elasticache create-replication-group \
  --region $PRIMARY_REGION \
  --replication-group-id $PRIMARY_CLUSTER_ID \
  --replication-group-description "Primary Redis cluster for AuctionFlow" \
  --num-cache-clusters 3 \
  --cache-node-type cache.r5.large \
  --cache-subnet-group-name $SUBNET_GROUP \
  --security-group-ids $SECURITY_GROUP \
  --engine redis \
  --engine-version 6.2 \
  --port 6379

# Create global datastore
aws elasticache create-global-replication-group \
  --global-replication-group-id $GLOBAL_DATASTORE_NAME \
  --primary-replication-group-id $PRIMARY_CLUSTER_ID \
  --primary-region $PRIMARY_REGION

# Add secondary regions
for REGION in "${SECONDARY_REGIONS[@]}"; do
  SECONDARY_CLUSTER_ID="auctionflow-redis-${REGION}"
  aws elasticache create-replication-group \
    --region $REGION \
    --replication-group-id $SECONDARY_CLUSTER_ID \
    --replication-group-description "Secondary Redis cluster in $REGION" \
    --num-cache-clusters 3 \
    --cache-node-type cache.r5.large \
    --cache-subnet-group-name $SUBNET_GROUP \
    --security-group-ids $SECURITY_GROUP \
    --global-replication-group-id $GLOBAL_DATASTORE_NAME \
    --engine redis \
    --engine-version 6.2 \
    --port 6379
done

echo "ElastiCache Global Datastore setup complete."