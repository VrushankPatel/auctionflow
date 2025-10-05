#!/bin/bash
set -euo pipefail

echo "========================================="
echo "AuctionFlow - Repository Reorganization"
echo "========================================="

# Create target directories
mkdir -p docs/api/legacy docs/specifications docs/operations
mkdir -p scripts/deployment scripts/backup scripts/setup scripts/testing
mkdir -p infrastructure/docker infrastructure/k8s infrastructure/helm infrastructure/monitoring/grafana
mkdir -p infrastructure/database/postgres infrastructure/storage/uploads
mkdir -p config

# 1) Documentation organization
# v1 spec preference: AUCTIONFLOW_API_SPECIFICATION.md > API_SPECIFICATION.md
if [ -f AUCTIONFLOW_API_SPECIFICATION.md ]; then
  git mv -f AUCTIONFLOW_API_SPECIFICATION.md docs/api/api-spec-v1.md || true
elif [ -f API_SPECIFICATION.md ]; then
  git mv -f API_SPECIFICATION.md docs/api/api-spec-v1.md || true
fi
if [ -f COMPREHENSIVE_API_SPECIFICATION.md ]; then
  git mv -f COMPREHENSIVE_API_SPECIFICATION.md docs/api/api-spec-comprehensive.md || true
fi
# Move remaining API spec variants to legacy (preserve history)
for f in API_SPEC.md AUCTIONFLOW_API_SPEC.md AUCTIONFLOW_COMPREHENSIVE_API_SPEC.md AUCTIONFLOW_COMPREHENSIVE_API_SPECIFICATION.md COMPREHENSIVE_API_SPEC.md; do
  if [ -f "$f" ]; then git mv -f "$f" docs/api/legacy/; fi
done
# Versioning doc
if [ -f API_VERSIONING.md ]; then git mv -f API_VERSIONING.md docs/api/api-versioning.md; fi

# Product and frontend specifications
for f in FRONTEND_SPECIFICATION.md PRODUCT_SPECIFICATION.md; do
  if [ -f "$f" ]; then git mv -f "$f" docs/specifications/; fi
done

# Operations docs
for f in DEPLOYMENT_GUIDE.md DISASTER_RECOVERY_RUNBOOK.md KIBANA_DASHBOARDS.md PERFORMANCE_BUDGETS.md; do
  if [ -f "$f" ]; then git mv -f "$f" docs/operations/; fi
done

# 2) Scripts organization
# Deployment
for f in deploy.sh canary-rollout.sh rollback.sh switch-to-blue.sh switch-to-green.sh; do
  if [ -f "$f" ]; then git mv -f "$f" scripts/deployment/; fi
done
# Backup
for f in kafka_backup.sh postgres_backup.sh redis_backup.sh postgres_pitr_restore.sh setup_backup_cron.sh; do
  if [ -f "$f" ]; then git mv -f "$f" scripts/backup/; fi
done
# Setup
for f in setup-geo-routing.sh setup-multi-region-db.sh setup-multi-region-kafka.sh setup-multi-region-redis.sh; do
  if [ -f "$f" ]; then git mv -f "$f" scripts/setup/; fi
done
# Testing
for f in test-api-integration.sh test-multi-region.sh test_backup_restore.sh; do
  if [ -f "$f" ]; then git mv -f "$f" scripts/testing/; fi
done

# 3) Infrastructure reorg
# Dockerfiles at root
for f in Dockerfile Dockerfile.frontend; do
  if [ -f "$f" ]; then git mv -f "$f" infrastructure/docker/; fi
done
# Compose files
for f in docker-compose.yml docker-compose.minimal.yml docker-compose.yml.broken docker-compose.minimal.yml.backup; do
  if [ -f "$f" ]; then git mv -f "$f" infrastructure/docker/; fi
done
# K8s and Helm
if [ -d k8s ]; then git mv -f k8s infrastructure/k8s; fi
if [ -d helm ]; then git mv -f helm infrastructure/helm; fi
# Monitoring
if [ -d grafana ]; then git mv -f grafana infrastructure/monitoring/grafana; fi
for f in prometheus.yml filebeat.yml; do
  if [ -f "$f" ]; then git mv -f "$f" infrastructure/monitoring/; fi
done
# Database
if [ -d postgres ]; then
  git mv -f postgres/* infrastructure/database/postgres/ 2>/dev/null || true
  rmdir postgres 2>/dev/null || true
fi

# 4) Storage uploads (shared)
if [ -d uploads ]; then
  # move contents if any
  if [ "$(ls -A uploads 2>/dev/null || true)" != "" ]; then
    git mv -f uploads/* infrastructure/storage/uploads/ || true
  fi
  rmdir uploads 2>/dev/null || true
fi

# Add .gitkeep to ensure dir exists
[ -f infrastructure/storage/uploads/.gitkeep ] || touch infrastructure/storage/uploads/.gitkeep

echo "Updating references (compose paths already patched in previous step if applicable)..."
# Nothing to sed here since we already patched docker-compose files in place before moving.

# 5) Cleanup build artifacts and logs from git index
# Remove common transient artifacts if tracked
set +e
patterns=("*.log" "*.log.*" "gc.log*" "*.pid" "build/" "dist/" "target/")
for p in "${patterns[@]}"; do
  git ls-files -z "$p" | xargs -0 git rm --cached -r 2>/dev/null || true
done
set -e

# 6) Fix nested auction-api structure if present
if [ -d auction-api/auction-api ]; then
  git rm -r --cached auction-api/auction-api 2>/dev/null || true
  rm -rf auction-api/auction-api || true
fi

# 7) Final status
echo "\nReorganization complete. Review changes with: git status"
echo "Then commit: git commit -m 'chore: repo reorg - docs/scripts/infrastructure layout'"
echo "If deploy script moved: use scripts/deployment/deploy.sh going forward."
