# Auction Flow On-Call Procedures

## Overview

This document outlines the procedures for on-call engineers supporting the Auction Flow platform. On-call responsibilities include monitoring system health, responding to alerts, and coordinating incident response.

## On-Call Schedule

### Rotation
- **Duration**: 1 week per engineer
- **Team**: SRE and Backend Engineering teams
- **Schedule**: Monday 9AM - Monday 9AM
- **Tool**: PagerDuty rotation schedule

### Coverage
- **Primary**: 24/7 coverage for SEV-1/2 incidents
- **Secondary**: Business hours (9AM-6PM PST) for SEV-3 issues
- **Holidays**: Designated holiday coverage engineer

## Tools and Access

### Required Access
- **PagerDuty**: Alert notifications and escalation
- **Slack**: #incidents, #alerts channels
- **Grafana**: Monitoring dashboards
- **Kibana**: Log aggregation and search
- **AWS/GCP Console**: Infrastructure access
- **Database**: Read-only access to production DB
- **SSH**: Access to bastion hosts

### Communication Channels
- **Alerts**: PagerDuty mobile app
- **Team Chat**: Slack #on-call
- **Status Updates**: Internal status page
- **User Communication**: Public status page (status.auctionflow.com)

## Daily On-Call Routine

### Morning Check (9AM)
1. **Review overnight alerts:**
   ```bash
   # Check PagerDuty incidents
   pd incidents list --statuses triggered,acknowledged

   # Review Grafana alerts
   # Access: https://grafana.auctionflow.com
   ```

2. **System health verification:**
   ```bash
   # Application health checks
   curl https://api.auctionflow.com/actuator/health

   # Database connectivity
   psql -h prod-db.auctionflow.com -U readonly -d auctionflow -c "SELECT 1;"

   # Redis health
   redis-cli -h prod-redis.auctionflow.com ping
   ```

3. **Performance metrics review:**
   - Bid latency trends
   - Error rates
   - Resource utilization

### Ongoing Monitoring
- **Alert Response**: Acknowledge within 5 minutes
- **Dashboard Monitoring**: Check every 2 hours
- **Log Review**: Investigate anomalies

### Evening Handover (6PM)
1. **Document ongoing issues**
2. **Update status of active incidents**
3. **Note any upcoming maintenance**

## Alert Response Procedures

### Alert Acknowledgment
1. **Acknowledge in PagerDuty** within 5 minutes
2. **Assess severity** and impact
3. **Notify team** in Slack if SEV-1/2
4. **Begin investigation** using runbooks

### Common Alert Patterns

#### High Error Rate Alert
```
Alert: HTTP 5xx errors > 5% for 5 minutes
```
**Response:**
1. Check application logs in Kibana
2. Identify failing endpoints
3. Check downstream dependencies (DB, Redis, Kafka)
4. Scale or restart affected services

#### Database Connection Pool Exhausted
```
Alert: DB connection pool usage > 90%
```
**Response:**
1. Check database performance metrics
2. Identify long-running queries
3. Scale application instances
4. Restart database connections if needed

#### High Bid Latency
```
Alert: P95 bid latency > 100ms
```
**Response:**
1. Check system resource usage (CPU, memory)
2. Monitor database query performance
3. Verify Redis cache effectiveness
4. Scale horizontally if load increasing

#### Timer Drift
```
Alert: Auction close timer drift > 30 seconds
```
**Response:**
1. Check system clock synchronization
2. Verify timer service health
3. Reconcile scheduled jobs
4. Restart timer service if unresponsive

## Incident Response Workflow

### For SEV-1/2 Incidents
1. **Immediate Assessment** (15 minutes)
   - Gather symptoms and impact
   - Check monitoring dashboards
   - Determine affected components

2. **Declare Incident** (30 minutes)
   - Create incident in PagerDuty
   - Notify incident response team
   - Update status page

3. **Coordinate Response**
   - Follow incident response runbook
   - Escalate to engineering lead if needed
   - Communicate updates every 30 minutes

4. **Resolution and Follow-up**
   - Implement fix or workaround
   - Conduct post-mortem within 48 hours
   - Update documentation

### Escalation Paths
- **Engineering Lead**: For complex technical issues
- **Product Manager**: For business impact assessment
- **DevOps Manager**: For infrastructure issues
- **External Vendors**: For third-party service outages

## Emergency Contacts

### Internal Team
- **SRE Lead**: John Doe (john.doe@auctionflow.com) - +1-555-0101
- **Engineering Manager**: Jane Smith (jane.smith@auctionflow.com) - +1-555-0102
- **DevOps Manager**: Bob Johnson (bob.johnson@auctionflow.com) - +1-555-0103

### External Vendors
- **Cloud Provider**: AWS Support - 1-888-555-1234
- **Database**: PostgreSQL Enterprise Support - support@postgresql.com
- **Payment Processor**: Stripe Support - support@stripe.com

## Maintenance and Deployments

### Scheduled Maintenance
- **Windows**: Tuesdays 2AM-4AM PST (low-traffic period)
- **Notification**: 48 hours advance notice
- **On-call**: Monitor during maintenance window

### Emergency Deployments
- **Approval**: Engineering lead approval required
- **Testing**: Must pass staging tests
- **Rollback Plan**: Required for all deployments

## Health and Well-being

### Best Practices
- **Sleep**: Get adequate rest during on-call week
- **Breaks**: Take regular breaks during long incidents
- **Support**: Reach out to team for help when needed
- **Documentation**: Keep detailed notes for handover

### Burnout Prevention
- **Rotation Frequency**: Maximum 1 week every 4 weeks
- **Shadowing**: New on-call engineers shadow experienced ones
- **Feedback**: Regular feedback sessions on on-call experience

## Training and Certification

### On-Call Training
- **Duration**: 2-week training period for new engineers
- **Components**:
  - System architecture overview
  - Monitoring and alerting tools
  - Incident response simulation
  - Runbook walkthrough

### Certification Requirements
- [ ] Complete system architecture training
- [ ] Pass incident response simulation
- [ ] Shadow 2 on-call rotations
- [ ] Demonstrate proficiency with monitoring tools

## Resources

### Documentation
- [Incident Response Runbook](Incident-Response-Runbook.md)
- [Troubleshooting Guide](Troubleshooting-Guide.md)
- [Deployment Guide](Deployment-Guide.md)
- [API Reference](API-Reference.md)

### Dashboards
- **Grafana**: https://grafana.auctionflow.com
- **Kibana**: https://kibana.auctionflow.com
- **Status Page**: https://status.auctionflow.com

### Tools
- **PagerDuty**: https://auctionflow.pagerduty.com
- **Slack**: https://auctionflow.slack.com
- **Jira**: https://auctionflow.atlassian.net

## Updates

This document is reviewed quarterly and updated as needed. Last updated: [Current Date]