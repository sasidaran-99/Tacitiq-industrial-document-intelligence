# TacitIQ — Unified Asset & Operations Brain

TacitIQ is an Enterprise AI Knowledge Intelligence Platform designed to preserve critical tribal knowledge, predict mechanical failures, audit safety procedures, and render real-time interactive 3D digital twins.

The platform combines Artificial Intelligence, Knowledge Graphs, Industrial Document Intelligence, Predictive Maintenance, and Digital Twin technologies into a unified operations platform that assists plant engineers, maintenance teams, and industrial operators in making data-driven decisions.

---

# ✨ Features

- 🔐 JWT Authentication & Role-Based Access Control
- 🤖 AI-powered Operations Assistant with Retrieval-Augmented Generation (RAG)
- 📄 Industrial Document Intelligence with OCR & Metadata Extraction
- 🕸 Neo4j Knowledge Graph Visualization
- 📊 Real-time Asset Health & Telemetry Dashboard
- ⚙️ Interactive 3D Digital Twin
- 📈 Predictive Maintenance & Failure Analysis
- 👷 Workforce Planning & Knowledge Loss Risk Assessment
- 📡 Live Event Streaming using WebSockets
- 🗄 PostgreSQL + pgvector for structured and vector data
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
- Authentication & Security
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
- **AI Engine:** Spring AI with Vertex AI Gemini
- **Database Migrations:** Flyway
- **Build Tool:** Maven

## Frontend Stack

- **Runtime:** React 18 + Vite + TypeScript
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
                      Spring Boot REST APIs
                                │
        ┌──────────────┬───────────────┬───────────────┐
        ▼              ▼               ▼               ▼
 PostgreSQL        Neo4j         Redis Cache      Spring AI
 (pgvector)     Knowledge Graph                   Gemini
```

---

# Folder Structure

```
tacitiq/
├── docker-compose.yml
├── README.md
├── backend/
│   ├── src/main/java/com/tacitiq/
│   │   ├── core/                  # Security, WebSockets, Event Configurations
│   │   └── modules/               # Authentication, Assets, AI, Search, Graph...
│   ├── src/main/resources/
│   │   ├── db/migration/          # Flyway Migrations
│   │   └── application.yml
│   └── pom.xml
│
└── frontend/
    ├── src/
    │   ├── App.tsx
    │   ├── components/
    │   └── index.css              # Enterprise Industrial UI Styling
    └── package.json
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

Backend supports running with or without Gemini.

Create a `.env` file (optional):

```env
GEMINI_API_KEY=your_api_key
```

If no Gemini API key is supplied, TacitIQ automatically falls back to **Offline Mock Mode**, allowing the entire platform to function for demonstrations and testing.

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
| POST | `/api/auth/login` | User Authentication | No |
| POST | `/api/auth/refresh` | Refresh JWT | No |
| GET | `/api/assets` | Retrieve Asset Registry | User |
| GET | `/api/assets/{id}/telemetry` | Live Telemetry | User |
| POST | `/api/documents/upload` | Upload Industrial Documents | Engineer |
| POST | `/api/agents/chat` | Operations Assistant | User |
| GET | `/api/agents/predict/{assetId}` | Predictive Maintenance | User |
| GET | `/api/agents/retirement-risk` | Workforce Planning | User |
| GET | `/api/graph/nodes` | Knowledge Graph | User |

---

# 📸 Screenshots

> Add screenshots after uploading them to the repository.

| Module | Screenshot |
|---------|------------|
| Login | `docs/images/login.png` |
| Asset Dashboard | `docs/images/dashboard.png` |
| Operations Assistant | `docs/images/chat.png` |
| Digital Twin | `docs/images/digital-twin.png` |
| Knowledge Graph | `docs/images/graph.png` |
| Document Intelligence | `docs/images/document-intelligence.png` |
| Workforce Planning | `docs/images/workforce.png` |

---

# 🎥 Demo

Demo Video:

```
Add your YouTube or Google Drive demo link here
```

Presentation:

```
Add your PPT link here
```

---

# Future Enhancements

- Real-time IoT Sensor Integration
- Remaining Useful Life (RUL) Prediction
- SAP / IBM Maximo Integration
- Semantic PDF Search
- Multi-Plant Asset Federation
- Advanced Failure Mode Detection
- LLM-powered Maintenance Report Generation
- Predictive Spare Parts Inventory

---

# License

This project is licensed under the MIT License.

---

# Developed For

Industrial Intelligence / Enterprise Knowledge Engineering / Asset Operations

Built using Java, Spring Boot, React, PostgreSQL, Neo4j, Redis, Spring AI, Three.js, and modern enterprise software engineering practices.
