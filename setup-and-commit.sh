#!/bin/bash

# Complete setup and commit script

echo "========================================="
echo "AuctionFlow - Setup and Commit"
echo "========================================="
echo ""

# Make all scripts executable
echo "Making scripts executable..."
chmod +x deploy.sh
chmod +x commit-changes.sh
chmod +x quick-update.sh
chmod +x rebuild-and-restart.sh
chmod +x build-and-run.sh
chmod +x test-api-integration.sh 2>/dev/null || true

echo "✓ Scripts are now executable"
echo ""

# Show git status
echo "Current git status:"
git status --short

echo ""
read -p "Do you want to commit these changes? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo ""
    echo "Adding all changes..."
    git add .
    
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
    echo "========================================="
    echo "✓ Commit completed successfully!"
    echo "========================================="
    echo ""
    echo "Next steps:"
    echo "  1. Push to remote: git push origin main (or your branch)"
    echo "  2. Deploy locally: ./deploy.sh quick"
    echo ""
else
    echo ""
    echo "Commit cancelled. You can commit manually later."
    echo ""
fi

echo "Available commands:"
echo "  ./deploy.sh quick    - Fast backend update"
echo "  ./deploy.sh full     - Complete rebuild"
echo "  ./deploy.sh logs     - View logs"
echo "  ./deploy.sh status   - Check status"
echo ""
echo "See DEPLOYMENT_GUIDE.md for more information"
echo ""
