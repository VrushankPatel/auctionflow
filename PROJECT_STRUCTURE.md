# Project Structure

This document explains the repository organization after re-structuring for clarity and maintainability.

## Top-level Layout

```
AuctionFlow/
├── auction-*/                      # Backend modules (Spring Boot/Java)
├── auctionflow-ui/                 # Frontend (client + server)
├── config/                         # Shared, non-secret configuration
├── docs/
│   ├── api/                        # API specifications and versioning
│   │   ├── api-spec-v1.md          # Current v1 REST API spec
│   │   ├── api-spec-comprehensive.md # Consolidated reference
│   │   ├── api-versioning.md       # Versioning policy
│   │   └── legacy/                 # Previous/duplicated specs retained for history
│   ├── operations/                 # Deployment, SRE, performance, dashboards
│   └── specifications/             # Product and frontend specifications
├── infrastructure/
│   ├── docker/                     # Dockerfiles and docker-compose*
│   │   ├── docker-compose.yml
│   │   └── docker-compose.minimal.yml
│   ├── database/
│   │   └── postgres/               # Init SQL and configs
│   ├── helm/                       # Helm charts
│   ├── k8s/                        # Kubernetes manifests
│   ├── monitoring/                 # Prometheus, Filebeat, Grafana
│   └── storage/
│       └── uploads/                # Shared uploads volume (gitignored)
├── scripts/
│   ├── deployment/                 # deploy.sh, canary, blue/green, rollback
│   ├── backup/                     # backup/restore and cron setup
│   ├── setup/                      # multi-region and environment setup
│   └── testing/                    # integration/system testing helpers
└── sdks/                           # Language SDKs (go, java, js, python)
```

## Conventions

- Run deployment via `./scripts/deployment/deploy.sh` from repo root.
- Shared storage for uploads is mounted at `infrastructure/storage/uploads/` on the host and `/app/uploads` in containers.
- All CI/CD, K8s, and Helm files live under `infrastructure/`.
- Docs are grouped by domain (API/specifications/operations) for easier discovery.

## Notes

- The reorg preserves git history by using `git mv` for files and directories.
- Old/duplicated API specs are maintained under `docs/api/legacy/` for reference.
- Build artifacts, logs, pids, and uploads are ignored across modules via `.gitignore`.
