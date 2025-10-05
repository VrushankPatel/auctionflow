#!/bin/bash
PERCENT=$1
BLUE_WEIGHT=$((100 - PERCENT))
GREEN_WEIGHT=$PERCENT

cat <<EOF | kubectl apply -f -
apiVersion: traefik.containo.us/v1alpha1
kind: IngressRoute
metadata:
  name: auction-api
spec:
  entryPoints:
    - web
  routes:
  - match: Host(\`auction-api.local\`)
    kind: Rule
    services:
    - name: auction-api-blue-service
      port: 80
      weight: $BLUE_WEIGHT
    - name: auction-api-green-service
      port: 80
      weight: $GREEN_WEIGHT
EOF