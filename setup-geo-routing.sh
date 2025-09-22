#!/bin/bash

# Script to set up AWS Global Accelerator for geo-routing
# Assumes AWS CLI configured

ACCELERATOR_NAME="auctionflow-accelerator"
REGIONS=("us-east-1" "eu-west-1" "ap-southeast-1")
LISTENER_PORT=80
ENDPOINT_PORT=80

# Create accelerator
ACCELERATOR_ARN=$(aws globalaccelerator create-accelerator \
  --name $ACCELERATOR_NAME \
  --ip-address-type IPV4 \
  --query 'Accelerator.AcceleratorArn' \
  --output text)

# Create listener
LISTENER_ARN=$(aws globalaccelerator create-listener \
  --accelerator-arn $ACCELERATOR_ARN \
  --port-ranges FromPort=$LISTENER_PORT,ToPort=$LISTENER_PORT \
  --protocol TCP \
  --query 'Listener.ListenerArn' \
  --output text)

# Add endpoint groups for each region
for REGION in "${REGIONS[@]}"; do
  # Assume ALB ARN exists, replace with actual
  ALB_ARN="arn:aws:elasticloadbalancing:$REGION:123456789012:loadbalancer/app/auctionflow-alb/1234567890123456"

  aws globalaccelerator create-endpoint-group \
    --listener-arn $LISTENER_ARN \
    --endpoint-group-region $REGION \
    --endpoint-configurations "Type=ALB,EndpointId=$ALB_ARN,Weight=100,ClientIPPreservationEnabled=true" \
    --traffic-dial-percentage 100 \
    --health-check-path "/actuator/health" \
    --health-check-port $ENDPOINT_PORT \
    --health-check-protocol HTTP \
    --threshold-count 3 \
    --interval-seconds 30
done

# Enable accelerator
aws globalaccelerator update-accelerator \
  --accelerator-arn $ACCELERATOR_ARN \
  --enabled

echo "AWS Global Accelerator setup complete. Accelerator ARN: $ACCELERATOR_ARN"