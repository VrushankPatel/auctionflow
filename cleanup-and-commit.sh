#!/bin/bash

echo "========================================="
echo "AuctionFlow - Cleanup and Commit"
echo "========================================="
echo ""

# Make deploy script executable
chmod +x deploy.sh

# Add all current changes
echo "Adding all changes..."
git add .

# Commit
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
- Added comprehensive DEPLOYMENT_GUIDE.md and RECENT_CHANGES.md

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
echo "✓ Commit completed!"
echo ""

# Remove unnecessary scripts
echo "Cleaning up unnecessary scripts..."
rm -f build-and-run.sh
rm -f build-deploy-run.sh
rm -f quick-update.sh
rm -f rebuild-and-restart.sh
rm -f commit-changes.sh
rm -f setup-and-commit.sh
rm -f cleanup-and-commit.sh  # Remove self

echo "✓ Cleanup completed!"
echo ""

# Commit the cleanup
git add .
git commit -m "chore: Remove redundant deployment scripts

Removed scripts (functionality now in deploy.sh):
- build-and-run.sh
- build-deploy-run.sh
- quick-update.sh
- rebuild-and-restart.sh
- commit-changes.sh
- setup-and-commit.sh
- cleanup-and-commit.sh

Kept scripts:
- deploy.sh (unified deployment)
- Backup/restore scripts (kafka_backup.sh, postgres_backup.sh, etc.)
- Multi-region setup scripts
- Test scripts"

echo ""
echo "========================================="
echo "✓ All done!"
echo "========================================="
echo ""
echo "Scripts kept:"
echo "  - deploy.sh (main deployment script)"
echo "  - Backup scripts (postgres_backup.sh, redis_backup.sh, etc.)"
echo "  - Setup scripts (setup-*.sh)"
echo "  - Test scripts (test-*.sh)"
echo ""
echo "Next steps:"
echo "  1. Push to remote: git push origin main"
echo "  2. Deploy: ./deploy.sh quick"
echo ""
