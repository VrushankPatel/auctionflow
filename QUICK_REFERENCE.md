# AuctionFlow Quick Reference Card

## 🚀 Most Common Commands

```bash
# Fast backend update (USE THIS MOST)
./scripts/deployment/deploy.sh quick

# View logs
./scripts/deployment/deploy.sh logs

# Check if everything is running
./scripts/deployment/deploy.sh status

# Complete rebuild (use sparingly)
./scripts/deployment/deploy.sh full
```

## 📋 All Deploy Commands

| Command | Time | Use When |
|---------|------|----------|
| `./scripts/deployment/deploy.sh quick` | 30s | Changed Java code ⭐ |
| `./scripts/deployment/deploy.sh frontend` | 1-2m | Changed UI code |
| `./scripts/deployment/deploy.sh backend` | 1m | Java + cache clear |
| `./scripts/deployment/deploy.sh full` | 2-3m | First time / major changes |
| `./scripts/deployment/deploy.sh restart` | 20s | Config changes only |
| `./scripts/deployment/deploy.sh logs` | - | View real-time logs |
| `./scripts/deployment/deploy.sh status` | - | Check health |
| `./scripts/deployment/deploy.sh clean` | - | Nuclear option 💣 |

## 🌐 Access URLs

- **UI**: http://localhost:8080/ui/
- **API**: http://localhost:8080/api/v1
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Health**: http://localhost:8080/actuator/health

## 🔧 Troubleshooting

### Problem: Build fails
```bash
./gradlew clean
./deploy.sh full
```

### Problem: Containers won't start
```bash
docker compose down -v
./deploy.sh full
```

### Problem: UI shows errors
```bash
./deploy.sh logs
# Check the output for errors
```

### Problem: 403 Forbidden
- Check filters are updated
- Verify SecurityConfig permits endpoints
- Run: `./deploy.sh quick`

### Problem: Blank page
- Check browser console
- Verify backend is running: `./deploy.sh status`
- Check logs: `./deploy.sh logs`

## 📝 Git Workflow

```bash
# First time setup
chmod +x setup-and-commit.sh
./setup-and-commit.sh

# Or manually
git add .
git commit -m "your message"
git push origin main
```

## 🏗️ Project Structure

```
AuctionFlow/
├── deploy.sh              ⭐ Main deployment script
├── DEPLOYMENT_GUIDE.md    📖 Full documentation
├── QUICK_REFERENCE.md     📋 This file
├── auction-api/           🔧 Backend (Java/Spring)
├── auctionflow-ui/        🎨 Frontend (React/TypeScript)
├── docker-compose.yml     🐳 Docker configuration
└── .gitignore            🚫 Ignored files
```

## 💡 Pro Tips

1. **Always use `quick` for Java changes** - It's the fastest
2. **Check logs when in doubt** - `./deploy.sh logs`
3. **Use `status` to verify** - `./deploy.sh status`
4. **Keep Docker running** - Don't quit Docker Desktop
5. **Full rebuild only when needed** - It takes longer

## 🎯 Daily Workflow

### Morning
```bash
git pull
./deploy.sh full
```

### During Development
```bash
# Make changes to Java code
./scripts/deployment/deploy.sh quick
# Test at http://localhost:8080/ui/
```

### Before Commit
```bash
./scripts/deployment/deploy.sh test
git add .
git commit -m "your changes"
```

## 🆘 Emergency Commands

```bash
# Everything is broken
docker compose down -v
./scripts/deployment/deploy.sh clean
./scripts/deployment/deploy.sh full

# Just restart everything
docker compose restart
./scripts/deployment/deploy.sh status

# View all container logs
docker compose logs -f

# Check specific service
docker compose logs auction-api -f
```

## 📊 Monitoring

```bash
# Check container status
docker compose ps

# Check resource usage
docker stats

# Check application health
curl http://localhost:8080/actuator/health

# Check API endpoint
curl http://localhost:8080/api/v1/auctions
```

## 🔐 Security Notes

- `.env` file is gitignored (contains secrets)
- `*.key`, `*.p12`, `*.jks` are gitignored
- Never commit passwords or API keys
- Use environment variables for sensitive data

## 📚 Documentation Files

- `DEPLOYMENT_GUIDE.md` - Complete deployment guide
- `RECENT_CHANGES.md` - Latest changes and fixes
- `QUICK_REFERENCE.md` - This cheat sheet
- `README.md` - Project overview

## 🎨 Frontend Development

```bash
cd auctionflow-ui
npm install
npm run dev  # Development mode
npm run build  # Production build
```

## 🔧 Backend Development

```bash
./gradlew :auction-api:build
./gradlew :auction-api:test
./gradlew clean
```

## 🐳 Docker Commands

```bash
# Start all services
docker compose up -d

# Stop all services
docker compose down

# View logs
docker compose logs -f

# Rebuild specific service
docker compose build auction-api

# Restart specific service
docker compose restart auction-api
```

---

**Remember**: When in doubt, use `./deploy.sh quick` for backend changes! 🚀
