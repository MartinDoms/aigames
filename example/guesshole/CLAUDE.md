# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Guesshole is a real-time multiplayer geography guessing game built with Spring Boot WebFlux backend and Vite frontend. Players watch videos and guess locations on a map, with scoring based on geographic accuracy.

## Architecture

**Backend**: Spring Boot 3.4.2 with WebFlux (reactive programming), R2DBC for database access, WebSockets for real-time communication
**Frontend**: Vite + Vanilla JavaScript + Alpine.js + Tailwind CSS  
**Database**: PostgreSQL 14+ with PostGIS extension for geographic calculations
**Real-time**: WebSocket-based multiplayer game state synchronization

## Development Commands

### Backend (Spring Boot)
```bash
./gradlew bootRun          # Start development server (port 8088)
./gradlew build            # Build project
./gradlew test             # Run tests
./gradlew flywayMigrate    # Run database migrations
```

### Frontend
```bash
# Via Gradle (recommended for full build)
./gradlew buildFrontend             # Build frontend assets
./gradlew startFrontendDevServer    # Start Vite dev server

# Direct npm commands (in frontend/ directory)
npm run dev                # Vite dev server (port 5173)
npm run build              # Production build
npm run lint               # ESLint
npm run format             # Prettier format
```

### Docker
```bash
docker-compose up          # Full stack with PostgreSQL, nginx, monitoring
```

## Key Architecture Patterns

**WebSocket Message Handling**: Centralized message routing in `LobbyWebSocketHandler` with specific handlers in `/websocket/handler/` directory

**Game State Management**: Reactive state updates via `GameStateService` with WebSocket broadcasts to all connected players

**Geographic Calculations**: PostGIS integration for distance calculations and location scoring in `GeographicDistanceCalculator`

**Frontend State**: Alpine.js components manage local state, WebSocket service handles server communication in `websocket-service.js`

**Database**: Flyway migrations in `/db/migration/`, R2DBC repositories for reactive database access

## Development Setup Requirements

- PostgreSQL 14+ with PostGIS extension
- Geographic boundary data import (GADM dataset)
- Node.js 22.14.0 (auto-installed via Gradle)

## Port Configuration

- Backend: 8088
- Metrics: 8081  
- Frontend dev server: 5173
- PostgreSQL: 5432 (in Docker)