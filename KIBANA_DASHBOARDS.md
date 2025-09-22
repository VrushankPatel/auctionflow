# Kibana Dashboards for Auction Flow

This document describes the Kibana dashboards to be created for log analysis and monitoring of the Auction Flow system.

## Prerequisites
- Elasticsearch and Kibana running via docker-compose
- Logs are shipped to Elasticsearch via Filebeat

## Creating Dashboards

1. Access Kibana at http://localhost:5601
2. Go to Management > Stack Management > Index Patterns
3. Create index pattern for `filebeat-*` to match the log indices

## Recommended Dashboards

### 1. Service Overview Dashboard
- **Purpose**: High-level view of all services' health and activity
- **Visualizations**:
  - Log level distribution (pie chart)
  - Logs per service over time (line chart)
  - Error rate by service (bar chart)
  - Top error messages (table)

### 2. Request Tracing Dashboard
- **Purpose**: Trace requests across services using correlation IDs
- **Visualizations**:
  - Request flow timeline (using correlationId field)
  - Response times by endpoint (histogram)
  - Failed requests (table with correlationId)

### 3. Error Analysis Dashboard
- **Purpose**: Deep dive into errors and exceptions
- **Visualizations**:
  - Error trends over time
  - Top exceptions by type
  - Error details with stack traces
  - Affected services and endpoints

### 4. Performance Dashboard
- **Purpose**: Monitor system performance metrics from logs
- **Visualizations**:
  - Request duration percentiles
  - Throughput by service
  - Slow queries or operations

## Sample Queries

### All errors from auction-api
```
service: "auction-api" AND level: "ERROR"
```

### Requests with correlation ID
```
correlationId: "some-uuid"
```

### High response times
```
duration > 1000
```

## Alerts
Set up alerts in Kibana for:
- High error rates (>5% in 5 minutes)
- Service down (no logs from service in 5 minutes)
- Slow responses (>2s average in 10 minutes)