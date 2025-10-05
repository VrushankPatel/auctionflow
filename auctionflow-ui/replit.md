# AuctionFlow - Real-time Auction Platform

## Overview

AuctionFlow is a modern, real-time auction platform built with React and Express. The application enables users to browse auctions, place bids, manage watchlists, and participate in live bidding with WebSocket-based real-time updates. The platform supports both buyers and sellers with role-based access control and comprehensive auction management features.

## User Preferences

Preferred communication style: Simple, everyday language.

## System Architecture

### Frontend Architecture

**Framework & Build Tool**
- React 18+ with TypeScript for type-safe component development
- Vite as the build tool and development server for fast hot module replacement
- Wouter for lightweight client-side routing

**UI Component System**
- ShadCN UI components built on Radix UI primitives for accessibility
- Tailwind CSS for utility-first styling with custom design tokens
- Custom theme system supporting light/dark modes via CSS variables
- Lucide React for consistent iconography

**State Management Strategy**
- **Server State**: TanStack Query (React Query) handles all API data fetching, caching, and synchronization
- **Client State**: Zustand manages UI state (modals, selections) and authentication state with persistence
- **Real-time State**: WebSocket service manages live auction updates and bid notifications

**Key Design Patterns**
- Centralized API service layer (`services/api.ts`) with mock data fallback
- Custom hooks for reusable logic (timers, WebSocket connections, mobile detection)
- Form validation using React Hook Form with Zod schemas
- Optimistic UI updates for better perceived performance

### Backend Architecture

**Server Framework**
- Express.js with TypeScript running on Node.js
- ESM module system for modern JavaScript features
- Vite middleware integration for seamless development experience

**API Design**
- RESTful API structure expected at `/api/v1/*` endpoints
- Mock data system (`services/mockData.ts`) allows frontend development without backend
- Fallback mechanism automatically uses mocks when backend is unavailable

**Real-time Communication**
- WebSocket server on `/ws` path for bidirectional communication
- Pub/sub pattern for auction-specific subscriptions
- Automatic reconnection logic with exponential backoff
- Message-based protocol for bid updates and notifications

**Session Management**
- In-memory storage implementation (`server/storage.ts`) for development
- Interface-based design allows easy database integration
- JWT-based authentication expected in production

### Data Storage Solutions

**Database Schema (Drizzle ORM)**
- PostgreSQL as the target database (via Neon serverless driver)
- Schema-first approach with TypeScript type generation
- Tables defined for users, auctions, bids, and watchlist

**Key Entities**
- **Users**: Authentication, roles (BUYER/SELLER/ADMIN), KYC status
- **Auctions**: Full auction lifecycle, pricing (starting/reserve/buyNow), status management
- **Bids**: Bidding history with idempotency keys, status tracking
- **Watchlist**: User-auction relationships for saved items

**Data Access Pattern**
- Drizzle ORM provides type-safe database queries
- Schema defined in `shared/schema.ts` for frontend/backend sharing
- Validation schemas auto-generated from Drizzle schema using drizzle-zod

### Authentication & Authorization

**Authentication Flow**
- JWT token-based authentication stored in Zustand with persistence
- Login/register endpoints expected at backend
- Token included in Authorization header for protected requests
- Session state synchronized across tabs via localStorage

**Role-Based Access**
- User roles: BUYER, SELLER, ADMIN
- Role determines UI access to create auction, dashboard features
- KYC status tracking for verification requirements

### External Dependencies

**Third-Party UI Libraries**
- @radix-ui/* family: Accessible primitive components (dialogs, dropdowns, tooltips, etc.)
- embla-carousel-react: Touch-friendly image carousels
- date-fns: Date formatting and manipulation
- class-variance-authority & clsx: Dynamic className generation

**Development Tools**
- @replit/vite-plugin-*: Replit-specific development enhancements
- drizzle-kit: Database schema migrations and management
- tsx: TypeScript execution for development server

**Real-time Infrastructure**
- ws (WebSocket): Native WebSocket implementation for server
- Custom WebSocket service with reconnection and subscription management

**Expected External Services**
- PostgreSQL database (Neon serverless recommended)
- Backend API at `http://localhost:8080/api/v1` (currently mocked)
- File upload service for auction images (placeholder URLs used)
- Payment provider integration (referenced in specification, not implemented)

**Performance Optimizations**
- Query caching via React Query with configurable stale times
- WebSocket connection pooling per auction
- Image lazy loading and responsive sizing
- Code splitting at route level for smaller bundle sizes