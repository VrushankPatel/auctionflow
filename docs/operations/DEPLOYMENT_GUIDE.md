# AuctionFlow Deployment Guide

## Quick Start

The `deploy.sh` script is your one-stop solution for all deployment needs.

### Make it executable (first time only)
```bash
chmod +x scripts/deployment/deploy.sh
```

## Common Commands

### 🚀 Full Deployment (First Time / Major Changes)
```bash
./scripts/deployment/deploy.sh full
```
Use when:
- Major changes to both frontend and backend
- After pulling major updates from git

### ⚡ Quick Backend Update (Most Common)
```bash
./scripts/deployment/deploy.sh quick
```
Use when:
- You changed Java code
- You fixed a bug
{{ ... }}
- **This is the fastest option for backend changes**

### 🎨 Frontend Only Update
```bash
./scripts/deployment/deploy.sh frontend
```
Use when:
- You only changed UI code (React/TypeScript)
- You updated styles or components

### 🔧 Backend Only Update
```bash
./scripts/deployment/deploy.sh backend
```
Use when:
- You changed Java code and want a full rebuild
- You want to clear Docker cache

### 🔄 Restart Containers
```bash
./scripts/deployment/deploy.sh restart
```
Use when:
- You changed configuration files
- You want to restart services without rebuilding

### 📊 View Logs
```bash
./scripts/deployment/deploy.sh logs
```
View real-time logs from the application

### 📈 Check Status
```bash
./scripts/deployment/deploy.sh status
```
Check if all services are running and healthy

### 🧪 Run Tests
```bash
./scripts/deployment/deploy.sh test
```
Run integration tests

### 🧹 Clean Everything
```bash
./scripts/deployment/deploy.sh clean
```
Stop containers and clean all build artifacts

## Typical Workflow

### Day-to-day Development
1. Make changes to Java code
2. Run: `./deploy.sh quick`
3. Test at http://localhost:8080/ui/

### UI Development
1. Make changes to React components
2. Run: `./deploy.sh frontend`
3. Test at http://localhost:8080/ui/

### After Git Pull
1. Pull latest changes
2. Run: `./deploy.sh full`
3. Verify everything works

## Troubleshooting

### If something goes wrong:
```bash
# Check logs
./deploy.sh logs

# Check status
./deploy.sh status

# Clean and restart
./deploy.sh clean
./deploy.sh full
```

### If containers won't start:
```bash
docker compose down -v
./deploy.sh full
```

### If build fails:
```bash
./gradlew clean
./deploy.sh full
```

## Access Points

After deployment:
- **UI**: http://localhost:8080/ui/
- **API**: http://localhost:8080/api/v1
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/actuator

## Environment Variables

Create a `.env` file in the root directory for custom configuration:

```env
POSTGRES_DB=auctionflow
POSTGRES_USER=auction_user
POSTGRES_PASSWORD=secure_password
SPRING_PROFILES_ACTIVE=dev
```

## Script Options Reference

| Command | Description | Use Case |
|---------|-------------|----------|
| `full` | Complete rebuild | First time, major changes |
| `quick` | Fast backend update | Daily development |
| `backend` | Backend only | Java code changes |
| `frontend` | Frontend only | UI changes |
| `restart` | Restart containers | Config changes |
| `test` | Run tests | Verify functionality |
| `logs` | View logs | Debugging |
| `status` | Check health | Verify deployment |
| `clean` | Clean all | Start fresh |

## Tips

1. **Use `quick` for most backend changes** - It's the fastest
2. **Use `full` sparingly** - Only when necessary
3. **Check logs if issues occur** - `./deploy.sh logs`
4. **Keep Docker running** - Don't stop Docker Desktop
5. **Monitor status** - Use `./deploy.sh status` to verify health

## Support

If you encounter issues:
1. Check logs: `./deploy.sh logs`
2. Check status: `./deploy.sh status`
3. Try clean restart: `./deploy.sh clean && ./deploy.sh full`
4. Check Docker: `docker compose ps`
