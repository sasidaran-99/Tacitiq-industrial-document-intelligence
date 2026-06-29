# TacitIQ — Unified Asset & Operations Brain

TacitIQ is an Enterprise AI Knowledge Intelligence Platform designed to preserve critical tribal knowledge, predict mechanical failures, audit safety procedures, and render real-time interactive 3D digital twins.

---

## Technical Architecture

### Backend Stack
* **Language & SDK**: Java 21 (Eclipse Temurin)
* **Framework**: Spring Boot 3.3.0 (Web, Security, Data JPA, SDN, WebSocket, Cache)
* **AI Engine**: Spring AI with Vertex AI Gemini model providers
* **Database Migrations**: Flyway Migrations
* **Build tool**: Maven

### Frontend Stack
* **Runtime**: React 19 SPA + Vite + TypeScript
* **Styling**: TailwindCSS
* **3D Viewer**: Three.js WebGL canvas
* **Graph Canvas**: SVG Node-Link interactive graph
* **Charts**: Recharts

### Database & Event Stack
* **Primary DB**: PostgreSQL 16 (configured with pgvector extension)
* **Knowledge Graph**: Neo4j 5.x
* **Caching**: Redis
* **Streaming**: Spring ApplicationEventPublisher + WebSocket (STOMP SockJS)

---

## Folder Structure

```
tacitiq/
├── docker-compose.yml
├── README.md
├── backend/
│   ├── src/main/java/com/tacitiq/
│   │   ├── core/                  # Web Security, WebSockets, Events configurations
│   │   └── modules/               # Bounded contexts (Auth, Asset, AI, Search, Graph...)
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway SQL schema and seed scripts
│   │   └── application.yml        # Configurations
│   └── pom.xml
└── frontend/
    ├── src/
    │   ├── App.tsx                # Base routing frame
    │   ├── components/            # ChatInterface, DigitalTwin3D, Recharts dashboards
    │   └── index.css              # Custom neon styling
    └── package.json
```

---

## Installation & Running Locally

### Prerequisites
* Java 21 JDK
* Maven 3.9+
* Node.js v20+
* Docker Desktop (for Postgres, Neo4j, Redis)

### Option 1: Running with Docker Compose (Recommended)
Spins up PostgreSQL, pgvector, Neo4j, Redis, Java backend, and Nginx frontend in one command:
```bash
docker-compose up --build
```
Once healthy, access the UI at: `http://localhost:3000`.

### Option 2: Running Services Separately
1. **Spin up databases only**:
   ```bash
   docker-compose up -d postgres neo4j redis
   ```
2. **Start Backend**:
   Set Gemini Key (Optional: system runs in robust Offline Mock Mode if undefined):
   ```bash
   export GEMINI_API_KEY=your_gemini_api_key
   cd backend
   mvn spring-boot:run
   ```
   Backend listens on: `http://localhost:8080`.
3. **Start Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   Vite dev server listens on: `http://localhost:3000` (proxied to port 8080).

---

## Pre-seeded Credentials
* **System Admin**: `admin@tacitiq.com` / `password` (default login prefilled)
* **Plant Manager**: `manager@tacitiq.com` / `password`
* **Maintenance Engineer**: `engineer@tacitiq.com` / `password`
* **HSE Officer**: `hse@tacitiq.com` / `password`
* **Reliability Engineer**: `reliability@tacitiq.com` / `password`

---

## REST Endpoints Overview

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/login` | Sign-in and fetch JWT access token | None |
| `POST` | `/api/auth/refresh` | Issue new access token using httpOnly cookie | None |
| `GET` | `/api/assets` | Retrieve all assets and health indices | User |
| `GET` | `/api/assets/{id}/telemetry` | Retrieve sensor time-series lines | User |
| `POST` | `/api/documents/upload` | Multipart file upload and MIME verify | Engineer/Manager |
| `POST` | `/api/agents/chat` | Submit RAG query to capture cited answers | User |
| `GET` | `/api/agents/predict/{assetId}` | Get failure probabilities & SHAP explanation | User |
| `GET` | `/api/agents/retirement-risk` | Get knowledge loss risk rankings | User |
| `GET` | `/api/graph/nodes` | Get network nodes & edges for graph explorer | User |
