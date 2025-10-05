#!/bin/bash

# Script to set up MSK with cross-region replication
# Assumes AWS CLI configured

PRIMARY_REGION="us-east-1"
SECONDARY_REGIONS=("eu-west-1" "ap-southeast-1")
CLUSTER_NAME="auctionflow-kafka"
KAFKA_VERSION="2.8.1"
NUMBER_OF_BROKERS=3
BROKER_INSTANCE_TYPE="kafka.m5.large"
SUBNETS="subnet-12345,subnet-67890"  # Assume exists
SECURITY_GROUPS="sg-12345"  # Assume exists

# Create MSK cluster in primary region
aws kafka create-cluster \
  --region $PRIMARY_REGION \
  --cluster-name $CLUSTER_NAME \
  --kafka-version $KAFKA_VERSION \
  --number-of-broker-nodes $NUMBER_OF_BROKERS \
  --broker-node-group-info "InstanceType=$BROKER_INSTANCE_TYPE,ClientSubnets=$SUBNETS,SecurityGroups=$SECURITY_GROUPS"

# Get cluster ARN
CLUSTER_ARN=$(aws kafka list-clusters --region $PRIMARY_REGION --query 'ClusterInfoList[0].ClusterArn' --output text)

# Create replicator for cross-region replication
for REGION in "${SECONDARY_REGIONS[@]}"; do
  # First, create a cluster in secondary region
  aws kafka create-cluster \
    --region $REGION \
    --cluster-name "${CLUSTER_NAME}-${REGION}" \
    --kafka-version $KAFKA_VERSION \
    --number-of-broker-nodes $NUMBER_OF_BROKERS \
    --broker-node-group-info "InstanceType=$BROKER_INSTANCE_TYPE,ClientSubnets=$SUBNETS,SecurityGroups=$SECURITY_GROUPS"

  TARGET_CLUSTER_ARN=$(aws kafka list-clusters --region $REGION --query 'ClusterInfoList[0].ClusterArn' --output text)

  # Create replicator
  aws kafka create-replicator \
    --region $PRIMARY_REGION \
    --replicator-name "replicator-to-${REGION}" \
    --kafka-clusters "Type=Source,BrokerString=$CLUSTER_ARN;Type=Target,BrokerString=$TARGET_CLUSTER_ARN" \
    --replication-info-list "SourceKafkaClusterArn=$CLUSTER_ARN,TargetKafkaClusterArn=$TARGET_CLUSTER_ARN,TargetCompressionType=NONE,SourceKafkaVersions=2.8.1,TargetKafkaVersions=2.8.1"
done

echo "MSK cross-region replication setup complete."