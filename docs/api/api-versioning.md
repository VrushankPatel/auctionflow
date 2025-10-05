# API Versioning Guide

## Overview

AuctionFlow API uses versioning to ensure backward compatibility and smooth transitions between API versions. This document explains the versioning strategy, how to use it, and migration guides.

## Versioning Strategy

### URL Versioning
All API endpoints are prefixed with the version number:
- Current version: v1
- Example: `POST /api/v1/auctions`

### Header Versioning
Clients can optionally specify the API version using the `X-API-Version` header:
- Header: `X-API-Version: v1`
- If not specified, defaults to the latest version (currently v1)

### Content Negotiation
Accept header can be used for content type versioning:
- `Accept: application/vnd.auctionflow.v1+json`

## Current Version (v1)

All endpoints are available under `/api/v1/`.

### Key Endpoints
- `POST /api/v1/auctions` - Create auction
- `GET /api/v1/auctions` - List auctions
- `POST /api/v1/auctions/{id}/bids` - Place bid
- And more...

## Deprecation Policy

When a version is deprecated:
- Deprecation notice in response headers: `Deprecation: true`
- Sunset header indicating removal date: `Sunset: Sat, 31 Dec 2025 23:59:59 GMT`
- Link header to migration guide: `Link: <https://docs.auctionflow.com/api/v2-migration>; rel="deprecation"`

## Backward Compatibility

- v1 endpoints will remain available for at least 12 months after v2 release
- Breaking changes will only occur in new major versions
- Minor versions (v1.1, v1.2) are backward compatible

## Migration to v2 (Future)

When v2 is released:
1. Update client code to use `/api/v2/` prefix
2. Review breaking changes in changelog
3. Test against v2 endpoints
4. Gradually migrate traffic

### Breaking Changes in v2 (Example)
- Auction creation requires additional validation
- Bid response format changed

## Best Practices

- Always specify version in URL
- Use header versioning for testing different versions
- Monitor deprecation headers
- Plan migrations during low-traffic periods

## Support

For migration assistance, contact support@auctionflow.com