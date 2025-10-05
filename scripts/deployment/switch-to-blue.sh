#!/bin/bash
kubectl patch service auction-api-service -p '{"spec":{"selector":{"app":"auction-api","version":"blue"}}}'