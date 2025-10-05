# Frontend-Backend Integration Summary

## Overview
This document summarizes the changes made to integrate the React frontend with the Java Spring Boot backend for AuctionFlow.

## Changes Made

### 1. Backend Changes

#### A. GlobalExceptionHandler (`auction-api/src/main/java/com/auctionflow/api/config/GlobalExceptionHandler.java`)
**Problem**: Exception handler was catching ALL requests including static resources, returning JSON errors instead of serving files.

**Fix**: Restricted `@ControllerAdvice` to only apply to API controllers:
```java
@RestControllerAdvice(basePackages = "com.auctionflow.api.controllers")
```

#### B. WebConfig (`auction-api/src/main/java/com/auctionflow/api/config/WebConfig.java`)
**Problem**: Resource handlers weren't properly configured for serving UI assets.

**Fixes**:
- Added proper `PathResourceResolver` for `/ui/assets/**`
- Configured SPA fallback for client-side routing
- Merged duplicate `WebMvcConfig` configuration
- Added view controllers for root and `/ui/` paths

#### C. AuthController (`auction-api/src/main/java/com/auctionflow/api/AuthController.java`)
**Problem**: API response format didn't match frontend expectations.

**Fixes**:
1. **Login Request**: Added `email` field support (frontend sends `email`, backend expected `username`)
   ```java
   public static class LoginRequest {
       private String username;
       private String email; // NEW: Support email field
       private String password;
   }
   ```

2. **Login Response**: Changed response format to match frontend expectations
   ```java
   // Frontend expects:
   {
     "token": "jwt-token",
     "user": {
       "id": "123",
       "email": "user@example.com",
       "displayName": "User Name",
       "role": "BUYER"
     }
   }
   ```

3. **Error Responses**: Standardized error format
   ```java
   Map.of("error", "ERROR_CODE", "message", "Human readable message")
   ```

### 2. Frontend Changes

#### A. App.tsx (`auctionflow-ui/client/src/App.tsx`)
**Problem**: React Router (wouter) wasn't configured for `/ui` base path.

**Fix**: Added custom location hook to handle base path:
```typescript
const useHashLocation = (): [string, (to: string) => void] => {
  const location = window.location.pathname;
  const base = "/ui";
  
  const path = location.startsWith(base) 
    ? location.slice(base.length) || "/" 
    : location;
  
  const navigate = (to: string) => {
    window.history.pushState({}, "", base + to);
    window.dispatchEvent(new PopStateEvent("popstate"));
  };
  
  return [path, navigate];
};
```

#### B. API Service (`auctionflow-ui/client/src/services/api.ts`)
**Status**: Already well-configured with:
- Proper API base URL (`/api/v1`)
- Auth token handling
- Mock data fallback for development
- Correct endpoint paths

### 3. Build & Deployment

#### A. Vite Config (`auctionflow-ui/vite.config.ts`)
**Status**: Already configured with `base: '/ui/'`

#### B. Deployment Scripts
Created comprehensive scripts:

1. **rebuild-and-restart.sh**: Full rebuild and deployment
   - Builds frontend
   - Copies to backend static resources
   - Builds backend
   - Rebuilds Docker images
   - Starts containers
   - Runs integration tests

2. **test-api-integration.sh**: API integration tests
   - Tests reference data endpoints
   - Tests authentication (register/login)
   - Tests protected endpoints
   - Tests UI serving

## API Endpoints Available

### Authentication
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/refresh` - Refresh token

### Reference Data
- `GET /api/v1/reference/categories` - Get auction categories
- `GET /api/v1/reference/bid-increments` - Get bid increment rules
- `GET /api/v1/reference/auction-types` - Get auction types
- `GET /api/v1/reference/extension-policies` - Get extension policies

### Auctions
- `GET /api/v1/auctions` - List auctions (with filters)
- `GET /api/v1/auctions/{id}` - Get auction details
- `POST /api/v1/auctions` - Create auction (auth required)
- `POST /api/v1/auctions/{id}/bids` - Place bid (auth required)
- `GET /api/v1/auctions/{id}/bids` - Get bid history
- `POST /api/v1/auctions/{id}/watch` - Add to watchlist (auth required)
- `DELETE /api/v1/auctions/{id}/watch` - Remove from watchlist (auth required)

### Users
- `POST /api/v1/users` - Create user
- `GET /api/v1/users/{id}/bids` - Get user's bids (auth required)

## How to Deploy

### Option 1: Full Rebuild (Recommended)
```bash
./rebuild-and-restart.sh
```

This will:
1. Stop all containers
2. Build frontend
3. Copy frontend to backend
4. Build backend
5. Rebuild Docker images
6. Start containers
7. Run integration tests

### Option 2: Quick Frontend Update
```bash
cd auctionflow-ui
npm run build
cd ..
cp -r auctionflow-ui/client/dist/* auction-api/src/main/resources/static/ui/
docker compose restart auction-api
```

### Option 3: Backend Only
```bash
./gradlew :auction-api:build -x test
docker compose build --no-cache auction-api
docker compose up -d auction-api
```

## Testing the Integration

### Manual Testing
1. Open browser to `http://localhost:8080/ui/`
2. Click "Sign In" button in header
3. Switch to "Register" tab
4. Fill in:
   - Full Name: Test User
   - Email: test@example.com
   - Password: password123
5. Click "Create Account"
6. Switch to "Sign In" tab
7. Login with same credentials
8. You should see user info in header

### Automated Testing
```bash
chmod +x test-api-integration.sh
./test-api-integration.sh
```

## Current Status

### ✅ Working
- UI serving (CSS, JS, HTML)
- React Router with `/ui` base path
- Authentication (register/login)
- Reference data endpoints
- API error handling
- CORS configuration
- Security configuration

### 🚧 Needs Implementation
1. **Auction Listing**: Backend returns auctions but frontend needs real data
2. **Auction Creation**: Form exists but needs backend integration testing
3. **Bidding**: Endpoint exists but needs real-time updates
4. **User Profile**: Endpoint may need implementation
5. **Watchlist**: Backend endpoints exist, frontend integration needed
6. **WebSocket**: Real-time bid updates not yet connected

### 🔄 Mock Data Fallback
The frontend has mock data fallback enabled when backend is unavailable:
- Set `VITE_USE_MOCKS=true` in `.env` to always use mocks
- Automatically falls back to mocks on network errors

## Environment Variables

### Backend (`.env` or `docker-compose.yml`)
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres-primary:5432/auctionflow
SPRING_DATASOURCE_USERNAME=auction_user
SPRING_DATASOURCE_PASSWORD=secure_password_here
SPRING_REDIS_HOST=redis-1
SPRING_REDIS_PORT=6379
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka-1:29092
JWT_SECRET=mySecretKey1234567890123456789012345678901234567890
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000
```

### Frontend (`.env` in `auctionflow-ui/`)
```env
VITE_USE_MOCKS=false
VITE_API_BASE_URL=/api/v1
```

## Troubleshooting

### Issue: CSS/JS files return 500 error
**Cause**: GlobalExceptionHandler catching static resource requests
**Fix**: Already fixed - handler now only applies to API controllers

### Issue: 404 Page Not Found in UI
**Cause**: React Router not configured for `/ui` base path
**Fix**: Already fixed - custom location hook added

### Issue: Login returns 401
**Possible Causes**:
1. User doesn't exist - register first
2. Wrong password
3. Backend not running - check `docker compose ps`
4. Database not initialized - check `docker compose logs postgres-primary`

### Issue: CORS errors
**Fix**: CORS already configured in SecurityConfig to allow all origins in development

### Issue: Token not being sent
**Check**: 
1. Token is stored in localStorage (check browser DevTools > Application > Local Storage)
2. Authorization header is being sent (check Network tab)
3. Token hasn't expired (JWT_EXPIRATION is 24 hours by default)

## Next Steps

1. **Run the deployment**:
   ```bash
   ./rebuild-and-restart.sh
   ```

2. **Test the integration**:
   - Register a new user
   - Login
   - Browse auctions (will show mock data initially)
   - Try creating an auction

3. **Implement remaining features**:
   - Connect auction listing to real backend data
   - Implement WebSocket for real-time updates
   - Add user profile page
   - Implement watchlist functionality
   - Add payment integration

4. **Monitor logs**:
   ```bash
   docker compose logs -f auction-api
   ```

## Support

If you encounter issues:
1. Check container logs: `docker compose logs auction-api`
2. Verify containers are running: `docker compose ps`
3. Test API directly: `curl http://localhost:8080/api/v1/reference/categories`
4. Check browser console for frontend errors
5. Run integration tests: `./test-api-integration.sh`
