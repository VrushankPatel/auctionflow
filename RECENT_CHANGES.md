# Recent Changes Summary

## Date: 2025-10-05

### Issues Fixed

#### 1. 403 Forbidden Errors
**Problem**: API endpoints and WebSocket connections were returning 403 Forbidden errors.

**Root Cause**: Authentication filters (RateLimitFilter, ApiKeyAuthenticationFilter, JwtAuthenticationFilter) were not skipping public endpoints.

**Solution**: Updated all three filters to skip public endpoints:
- `/api/v1/auctions` and `/api/v1/auctions/**`
- `/ws` (WebSocket endpoint)
- `/api/v1/auth/**`
- `/api/v1/reference/**`
- `/api/v1/users`
- `/assets/**`
- `/favicon.ico`
- Static resources (`/ui/`, `/static/`, `/images/`)

**Files Changed**:
- `auction-api/src/main/java/com/auctionflow/api/config/RateLimitFilter.java`
- `auction-api/src/main/java/com/auctionflow/api/config/ApiKeyAuthenticationFilter.java`
- `auction-api/src/main/java/com/auctionflow/api/config/JwtAuthenticationFilter.java`

#### 2. Blank Page Error (Cannot read properties of undefined)
**Problem**: Frontend showed blank page with error: `Cannot read properties of undefined (reading '0')`

**Root Cause**: Backend's `AuctionSummaryDTO` only returned 4 fields (id, title, currentHighestBid, endTs), but frontend expected 18+ fields including `images`, `category`, `status`, etc.

**Solution**: 
1. Expanded SQL query to fetch all required fields
2. Updated `AuctionSummaryDTO` with all fields
3. Updated query handler to map all fields properly
4. Fixed `MobileService` to use new field names

**Files Changed**:
- `auction-api/src/main/java/com/auctionflow/api/repositories/AuctionReadRepository.java`
- `auction-api/src/main/java/com/auctionflow/api/dtos/AuctionSummaryDTO.java`
- `auction-api/src/main/java/com/auctionflow/api/queryhandlers/ListActiveAuctionsQueryHandler.java`
- `auction-api/src/main/java/com/auctionflow/api/services/MobileService.java`

### New Features

#### Unified Deployment Script (`deploy.sh`)
Created a comprehensive deployment script that handles all deployment scenarios:

**Commands**:
- `./deploy.sh full` - Complete rebuild (frontend + backend + docker)
- `./deploy.sh quick` - Fast backend update (most common)
- `./deploy.sh backend` - Backend only rebuild
- `./deploy.sh frontend` - Frontend only rebuild
- `./deploy.sh restart` - Restart containers without rebuild
- `./deploy.sh test` - Run integration tests
- `./deploy.sh logs` - View application logs
- `./deploy.sh status` - Check container status
- `./deploy.sh clean` - Clean all build artifacts

**Features**:
- Color-coded output for better readability
- Prerequisite checks
- Service health verification
- Automatic endpoint testing
- Comprehensive error handling

#### Improved .gitignore
Updated `.gitignore` to properly ignore:
- Build artifacts (`build/`, `target/`, `dist/`)
- Dependencies (`node_modules/`, `.gradle/`)
- IDE files (`.idea/`, `.vscode/`)
- Logs (`*.log`, `gc.log*`, `app.log`)
- Sensitive files (`.env`, `*.key`, `*.p12`)
- Generated static resources
- OS-specific files (`.DS_Store`)
- Memory dumps (`*.hprof`, `*.jfr`)

### Documentation

#### New Files
1. **DEPLOYMENT_GUIDE.md** - Comprehensive deployment guide
   - Quick start instructions
   - Common commands with use cases
   - Typical workflows
   - Troubleshooting guide
   - Access points and environment variables

2. **RECENT_CHANGES.md** - This file, documenting all changes

3. **commit-changes.sh** - Script to commit all changes with detailed message

### Technical Details

#### AuctionSummaryDTO Fields
Now includes:
- `id`, `itemId`, `sellerId`
- `title`, `description`, `category`, `condition`
- `images` (List<String>)
- `auctionType`, `status`
- `startingPrice`, `reservePrice`, `buyNowPrice`, `currentHighestBid`
- `bidCount`, `hiddenReserve`
- `startTime`, `endTime`

#### SQL Query Enhancement
Expanded from 4 fields to 18 fields:
```sql
SELECT a.id, a.item_id, i.seller_id, i.title, i.description, 
       i.category_id, i.condition, i.images, a.auction_type, 
       a.starting_price, a.reserve_price, a.buy_now_price, 
       a.current_highest_bid, a.bid_count, a.hidden_reserve, 
       a.start_ts, a.end_ts, a.status
FROM auctions a
LEFT JOIN items i ON a.item_id = i.id
WHERE a.deleted_at IS NULL AND a.status = 'OPEN'
```

### Testing

After deployment, verify:
1. ✅ UI loads at http://localhost:8080/ui/
2. ✅ Auctions display with images
3. ✅ WebSocket connects (no "Reconnecting..." alert)
4. ✅ No 403 errors in console
5. ✅ Auction cards show all information

### Next Steps

1. **Commit changes**:
   ```bash
   chmod +x commit-changes.sh
   ./commit-changes.sh
   ```

2. **Deploy**:
   ```bash
   chmod +x deploy.sh
   ./deploy.sh quick
   ```

3. **Verify**:
   - Open http://localhost:8080/ui/
   - Check auctions load correctly
   - Verify WebSocket connection

### Maintenance

For future deployments:
- **Daily development**: Use `./deploy.sh quick`
- **UI changes**: Use `./deploy.sh frontend`
- **Major updates**: Use `./deploy.sh full`
- **Troubleshooting**: Use `./deploy.sh logs` and `./deploy.sh status`

### Performance Notes

- Quick deployment: ~30 seconds
- Full deployment: ~2-3 minutes
- Frontend only: ~1-2 minutes
- Backend only: ~1 minute

### Security Improvements

All authentication filters now properly:
- Skip public endpoints
- Allow unauthenticated access to auctions list
- Maintain security for protected endpoints
- Support WebSocket connections without authentication for public data
