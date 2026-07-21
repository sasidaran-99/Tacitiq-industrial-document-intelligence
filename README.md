# TacitIQ — Unified Asset & Operations Brain

TacitIQ is an Enterprise AI Knowledge Intelligence Platform designed to preserve critical tribal knowledge, predict mechanical failures, audit safety procedures, and render real-time interactive 3D digital twins.

The platform combines Artificial Intelligence, Knowledge Graphs, Industrial Document Intelligence, Predictive Maintenance, and Digital Twin technologies into a unified operations platform that assists plant engineers, maintenance teams, and industrial operators in making data-driven decisions.

---

# ✨ Features

- 🔐 Enterprise Authentication (JWT + Google OAuth + Role-Based Access Control)
- 🤖 AI-powered Operations Assistant with Enterprise SaaS Workspace
- 💬 Smart Query Suggestions & Session History
- 🚨 Live Operational Context (Alerts, Connected AI Agents & Facility Health)
- 📄 Industrial Document Intelligence with OCR & Metadata Extraction
- 🕸 Interactive Neo4j Knowledge Graph Visualization
- 📊 Real-time Asset Health & Telemetry Dashboard
- ⚙️ Interactive 3D Digital Twin
- 📈 Predictive Maintenance & Failure Analysis
- 👷 Workforce Planning & Knowledge Loss Risk Assessment
- 📡 Live Event Streaming using WebSockets
- 🗄 PostgreSQL + pgvector for Structured & Vector Data
- 🔍 AI-assisted Asset Search & Root Cause Analysis

---

# 🏗 Project Modules

- Operations Assistant
- Asset Dashboard
- Digital Twin
- Knowledge Graph
- Document Intelligence
- Workforce Planning
- Live Event Console
- Authentication & Security (Local JWT + Google OAuth Identity Provider)
- Predictive Maintenance Engine

---

# Technical Architecture

## Backend Stack

- **Language & SDK:** Java 21 (Eclipse Temurin)
- **Framework:** Spring Boot 3.3.0
  - Spring Web
  - Spring Security
  - Spring Data JPA
  - Spring Data Neo4j
  - Spring WebSocket
  - Spring Cache
- **Token Verification:** Nimbus JOSE JWT Library (JWKS Verification)
- **AI Engine:** Spring AI with Vertex AI Gemini
- **Database Migrations:** Flyway
- **Build Tool:** Maven

## Frontend Stack

- **Runtime:** React 18 + Vite + TypeScript
- **OAuth Library:** `@react-oauth/google` SDK
- **Styling:** TailwindCSS
- **3D Viewer:** Three.js
- **Knowledge Graph:** SVG Interactive Node-Link Graph
- **Charts:** Recharts

## Database & Event Stack

- **Primary Database:** PostgreSQL 16 + pgvector
- **Knowledge Graph:** Neo4j 5.x
- **Caching:** Redis
- **Streaming:** Spring ApplicationEventPublisher + STOMP WebSockets (SockJS)

---

# System Architecture

```
                             React + Vite Frontend
                                      │
                                      ▼
                      Enterprise Authentication Layer
                     (Google OAuth + JWT + RBAC)
                                      │
                                      ▼
                           Spring Boot REST APIs
                                      │
          ┌──────────────┬───────────────┬───────────────┬──────────────┐
          ▼              ▼               ▼               ▼              ▼
     PostgreSQL      Neo4j         Redis Cache      Spring AI     WebSockets
     (pgvector)   Knowledge Graph                   Gemini       Live Events
```
---

# Folder Structure

```text
tacitiq/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/tacitiq/
│   │   │   │       ├── core/                 # Security, Configuration, WebSockets
│   │   │   │       └── modules/              # Authentication, Assets, AI, Graph, Documents...
│   │   │   └── resources/
│   │   │       ├── db/
│   │   │       │   └── migration/            # Flyway Migrations
│   │   │       └── application.yml
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/                       # Reusable UI Components
│   │   ├── App.tsx
│   │   ├── main.tsx
│   │   ├── index.css
│   │   └── vite-env.d.ts
│   ├── public/
│   ├── Dockerfile
│   ├── nginx.conf
│   ├── package.json
│   ├── tailwind.config.js
│   ├── vite.config.ts
│   └── .env.example
│
├── docs/
├── docker-compose.yml
├── LICENSE
└── README.md
```

---

# Installation & Running Locally

## Prerequisites

- Java 21 JDK
- Maven 3.9+
- Node.js v20+
- Docker Desktop

---

## Option 1 — Docker Compose (Recommended)

Starts the complete platform:

- PostgreSQL
- pgvector
- Neo4j
- Redis
- Spring Boot Backend
- React Frontend

```bash
docker compose up --build
```

Application:

```
http://localhost:3000
```

---

## Option 2 — Run Services Individually

### Start Databases

```bash
docker compose up -d postgres neo4j redis
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

Runs at:

```
http://localhost:8080
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs at:

```
http://localhost:3000
```

---

# 🔑 Environment Variables

Create a `.env` file:

```env
# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id
VITE_GOOGLE_CLIENT_ID=your_google_client_id

# Vertex AI / Gemini
GEMINI_API_KEY=your_gemini_api_key
```

### Authentication

TacitIQ supports two authentication methods:

- Google OAuth (Recommended)
- Local JWT Authentication using pre-seeded enterprise accounts

If Google OAuth credentials are not configured, the application automatically falls back to local authentication for development and demonstration purposes.

If no Gemini API key is configured, the AI assistant automatically runs in Offline Mock Mode.
---

# Pre-seeded Credentials

| Role | Email | Password |
|------|-------|----------|
| System Admin | admin@tacitiq.com | password |
| Plant Manager | manager@tacitiq.com | password |
| Maintenance Engineer | engineer@tacitiq.com | password |
| HSE Officer | hse@tacitiq.com | password |
| Reliability Engineer | reliability@tacitiq.com | password |

---

# REST Endpoints Overview

| Method | Endpoint | Description | Auth |
|---------|----------|-------------|------|
| POST | `/api/auth/login` | Local JWT Authentication | No |
| POST | `/api/auth/google` | Google OAuth Authentication | No |
| POST | `/api/auth/refresh` | Refresh JWT Token | No |
| GET | `/api/assets` | Retrieve Asset Registry | User |
| GET | `/api/assets/{id}/telemetry` | Live Telemetry | User |
| POST | `/api/documents/upload` | Upload Industrial Documents | Engineer |
| POST | `/api/agents/chat` | AI Operations Assistant | User |
| GET | `/api/agents/predict/{assetId}` | Predictive Maintenance | User |
| GET | `/api/agents/retirement-risk` | Workforce Planning | User |
| GET | `/api/graph/nodes` | Knowledge Graph | User |
---

# 📸 Screenshots


| Module | Screenshot |
|---------|------------|
| Login | `docs/images/login.png` |
| Asset Dashboard | `docs/images/dashboard.png` |
| Operations Assistant (Chat 1) | `docs/images/chat1.png` |
| Operations Assistant (Chat 2) | `docs/images/chat2.png` |
| Digital Twin | `docs/images/digital-twin.png` |
| Knowledge Graph | `docs/images/graph.png` |
| Document Intelligence | `docs/images/document-intelligence.png` |
| Workforce Planning | `docs/images/workforce.png` |

---

# 📄 Project Report

Access the complete project documentation below:

📘 **Project Report (PDF):**  
[TacitIQ Project Report](docs/TacitIQ_Project_Report.pdf)

---

# Future Enhancements

- Real-time IoT Sensor Integration
- Remaining Useful Life (RUL) Prediction
- SAP / IBM Maximo Integration
- Semantic PDF Search
- Multi-Plant Asset Federation
- Advanced Failure Mode Detection
- Streaming AI Responses
- Voice-enabled Operations Assistant
- LLM-powered Maintenance Report Generation
- Predictive Spare Parts Inventory


