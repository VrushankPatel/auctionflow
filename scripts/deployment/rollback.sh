#!/bin/bash
# Check if green deployment is healthy
READY=$(kubectl get deployment auction-api-green -o jsonpath='{.status.readyReplicas}')
REPLICAS=$(kubectl get deployment auction-api-green -o jsonpath='{.spec.replicas}')

if [ "$READY" -lt "$REPLICAS" ]; then
  echo "Green deployment unhealthy, rolling back to blue"
  ./switch-to-blue.sh
else
  echo "Green deployment healthy"
fi