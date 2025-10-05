#!/bin/bash

# Script to commit all recent changes

echo "========================================="
echo "Committing AuctionFlow Changes"
echo "========================================="
echo ""

# Make deploy script executable
chmod +x deploy.sh

# Show what will be committed
echo "Files to be committed:"
git status --short

echo ""
echo "Adding all changes..."
git add -A

echo ""
echo "Creating commit..."
git commit -m "feat: Fix API/WebSocket 403 errors and improve deployment

Changes:
- Fixed authentication filters to allow public endpoints (/api/v1/auctions, /ws, /favicon.ico)
- Updated RateLimitFilter, ApiKeyAuthenticationFilter, and JwtAuthenticationFilter
- Expanded AuctionSummaryDTO to include all required fields (images, category, status, etc.)
- Updated SQL query to fetch complete auction data
- Fixed MobileService to use new DTO field names
- Created unified deploy.sh script for all deployment scenarios
- Updated .gitignore to ignore build artifacts, node_modules, logs, and sensitive files
- Added comprehensive DEPLOYMENT_GUIDE.md

Fixes:
- 403 Forbidden errors on /api/v1/auctions endpoint
- WebSocket connection failures
- Blank page error (Cannot read properties of undefined)
- Missing auction images and metadata in UI

Deployment:
- Use ./deploy.sh quick for fast backend updates
- Use ./deploy.sh full for complete rebuild
- See DEPLOYMENT_GUIDE.md for all options"

echo ""
echo "========================================="
echo "Commit completed successfully!"
echo "========================================="
echo ""
echo "Next steps:"
echo "  1. Push to remote: git push origin main"
echo "  2. Or deploy locally: ./deploy.sh quick"
echo ""
