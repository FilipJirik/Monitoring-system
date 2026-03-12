<p align="center">
  <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="64" height="64" rx="16" fill="#111113"/>
    <rect x="12" y="16" width="40" height="26" rx="3" stroke="#F4F4F5" stroke-width="4"/>
    <path d="M32 42V50M24 50H40" stroke="#F4F4F5" stroke-width="4" stroke-linecap="round"/>
    <path d="M18 31H24L28 23L34 37L38 31H46" stroke="#F4F4F5" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>

  <h1 align="center">Monitoring System</h1>
  <p align="center">
    Application for collecting, monitoring, and visualizing system metrics from Linux devices.
    <br />
    Contains a <strong>Spring Boot</strong> backend API and a <strong>React</strong> frontend.
    <br />
    Part of the <strong>Aplikace pro sběr, monitoring a vizualizaci dat počítačů a serverů</strong> project.
  </p>
<p align="center">
  <a href="README-CS.md">🇨🇿 Česká verze</a>
</p>

---

## About

This repository contains the **Backend API** (Spring Boot) and the **Frontend** (React) for a device monitoring platform. The entire stack — PostgreSQL, Backend, and Frontend can be deployed as a single unit using Docker Compose.

### What it does

- **Metrics ingestion** — REST endpoints for receiving system metrics (CPU, RAM, disk, network, etc.) from agents authenticated via API keys.
- **Data aggregation** — Background jobs (via JobRunr) automatically roll up raw metrics into hourly and daily summaries for historical data.
- **Alerting** — Monitors incoming metrics against user-defined thresholds. Sends notifications via email (SMTP) and real-time via WebSocket to the frontend.
- **User management** — Role-based access control (Admin / User) with per-device permissions. Users can only see devices they've been granted access to.
- **Device management** — Registration, online/offline status tracking, device info, images, and API key management.

---

## Tech Stack

### Backend

| Layer | Technology |
|---|---|
| Runtime | Java 21 / Spring Boot 3.5 |
| Database | PostgreSQL |
| Security | Spring Security + JWT (stateless) |
| Data Access | Spring Data JPA + Hibernate |
| Caching | Caffeine |
| Background Jobs | JobRunr |
| Real-time | WebSocket (STOMP) |
| Email | Spring Mail (SMTP) |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Validation | Jakarta Validation + custom |

### Frontend

| Layer | Technology |
|---|---|
| Framework | React 19 (Vite + TypeScript) |
| Styling | Tailwind CSS 4 |
| Charts | Recharts |
| Maps | Leaflet |
| State | Zustand |
| Real-time | STOMP WebSocket |

### Infrastructure

| Component | Role |
|---|---|
| Docker Compose | Container orchestration |
| Nginx | Reverse proxy, static file serving, SSL termination |

---

## Client Agent

A lightweight **.NET 9** Linux daemon that collects system metrics and sends them to the backend. Supports automatic hardware detection, configurable intervals, and self-signed certificates. Available on [GitHub](https://github.com/FilipJirik/Monitoring-system-agent).

---

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose**
- **OpenSSL** — for generating self-signed SSL certificates

> For development without Docker, you'll also need **Java 21** (JDK), **Maven** (or the included `mvnw` wrapper), and **Node.js**.

### Configuration

Create a `.env` file in the project root:

```env
# Database
DB_USERNAME=postgres
DB_PASSWORD=your_db_password

# Security
JWT_SECRET=your_strong_jwt_secret
APP_ADMIN_EMAIL=admin@example.com
APP_ADMIN_PASSWORD=your_admin_password

# Email notifications (Gmail SMTP)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

> On first startup, an admin account is created automatically using `APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD`.

### Generating SSL Certificates

Before running the application, generate a self-signed certificate for Nginx. The certificate files must be placed in `frontend/certs/`:

```bash
cd frontend/certs
openssl req -x509 -nodes -days 365 -newkey rsa:2048 -keyout nginx.key -out nginx.crt
```

### Running Locally

Start the entire stack with Docker Compose:

```bash
docker compose up --build -d
```

This starts three containers: **PostgreSQL**, **Backend** (Spring Boot), and **Frontend** (Nginx).

The application will be available at **`https://localhost`**.

> Your browser will show a certificate warning because the SSL certificate is self-signed.

**Useful links:**
- **Application:** `https://localhost`
- **Swagger UI:** `https://localhost/api/swagger-ui/index.html`
- **JobRunr Dashboard:** `http://localhost:8000/dashboard`

### Standalone Development Mode

For development without Docker, you can run the backend and frontend individually:

1. **Start PostgreSQL:**
   ```bash
   docker compose up postgres -d
   ```
2. **Start the backend:**
   ```bash
   ./mvnw spring-boot:run
   ```
   The API starts at `http://localhost:8080`.

3. **Start the frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
   The dev server starts at `http://localhost:5173`.

---

## Gmail SMTP Setup

To enable email alerts, you need a valid email account for sending notifications. For a Gmail account, you need a Gmail **App Password**.

1. Go to [Google Account Security](https://myaccount.google.com/security).
2. Make sure **2-Step Verification** is turned **ON**.
3. Search for **"App passwords"** in the Google Account search bar (or go to [App Passwords](https://myaccount.google.com/apppasswords) directly).
4. Click **Create a new app password**, give it a name like `Monitoring App`.
5. Google will generate a **16-character code** - copy it.
6. Paste it as `MAIL_PASSWORD` in your `.env` file:

   ```env
   MAIL_USERNAME=your_email@gmail.com
   MAIL_PASSWORD=abcd efgh ijkl mnop
   ```

---

## Screenshots

---

## API Endpoints

### Authentication (`/api/auth`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register a new user account |
| POST | `/api/auth/login` | Authenticate and get JWT tokens |
| POST | `/api/auth/logout` | Invalidate the current session |
| POST | `/api/auth/refresh` | Refresh an expired JWT token |
| GET | `/api/auth/me` | Get current user info |
| POST | `/api/auth/change-password` | Change password |

### Devices (`/api/devices`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/devices` | Register a new device |
| GET | `/api/devices` | List accessible devices (paginated, searchable) |
| GET | `/api/devices/{id}` | Get device details |
| PUT | `/api/devices/{id}` | Update device info |
| DELETE | `/api/devices/{id}` | Delete a device |
| POST | `/api/devices/{id}/image` | Upload device picture |
| DELETE | `/api/devices/{id}/image` | Reset picture to default |
| POST | `/api/devices/{id}/regenerate-api-key` | Regenerate API key |

### Metrics (`/api/devices/{deviceId}/metrics`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/devices/{deviceId}/metrics` | Submit metrics (API key auth) |
| GET | `/api/devices/{deviceId}/metrics/latest` | Get latest snapshot |
| GET | `/api/devices/{deviceId}/metrics/history` | Get historical data (by type and period) |

### Alerts (`/api/alerts`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/alerts` | List alerts (filterable by status, severity) |
| POST | `/api/alerts/{id}/resolve` | Resolve an alert |
| DELETE | `/api/alerts/{id}` | Delete an alert |

### Thresholds (`/api/devices/{deviceId}/thresholds`)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/devices/{deviceId}/thresholds` | List thresholds for a device |
| POST | `/api/devices/{deviceId}/thresholds` | Create a threshold rule |
| PUT | `/api/devices/{deviceId}/thresholds/{id}` | Update a threshold rule |
| DELETE | `/api/devices/{deviceId}/thresholds/{id}` | Delete a threshold rule |

### Alert Recipients (`/api/devices/{deviceId}/recipients`)

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/devices/{deviceId}/recipients` | Add a notification recipient |
| GET | `/api/devices/{deviceId}/recipients` | List recipients |
| PUT | `/api/devices/{deviceId}/recipients/{userId}` | Update recipient settings |
| DELETE | `/api/devices/{deviceId}/recipients/{userId}` | Remove a recipient |

### Users (`/api/users`) — Admin only

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/users` | Create a user |
| GET | `/api/users` | List users (searchable) |
| GET | `/api/users/{id}` | Get user details |
| PUT | `/api/users/{id}` | Update user info |
| DELETE | `/api/users/{id}` | Delete a user |
| POST | `/api/users/{userId}/devices/{deviceId}` | Grant device access |
| PUT | `/api/users/{userId}/devices/{deviceId}` | Update device permissions |
| DELETE | `/api/users/{userId}/devices/{deviceId}` | Revoke device access |

### Settings (`/api/settings`) — Admin only

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/settings` | Get system settings |
| PUT | `/api/settings` | Update system settings |

> Full interactive API docs are available at `/api/swagger-ui/index.html`

---

## Project Structure

```
monitoring-system/
├── src/main/java/cz/jirikfi/monitoringsystembackend/
│   ├── components/      # Specialized Spring components
│   ├── configurations/  # Security, Cache, WebSocket, OpenAPI configs
│   ├── controllers/     # REST API controllers
│   ├── entities/        # JPA entities (Metrics, Device, User, Alert, etc.)
│   ├── enums/           # Enumerations (Role, MetricType, AlertSeverity, etc.)
│   ├── exceptions/      # Custom exception classes
│   ├── mappers/         # DTO <-> Entity mapping
│   ├── middleware/      # JWT and API Key authentication filters
│   ├── models/          # DTOs and request/response models
│   ├── repositories/    # Spring Data JPA repositories
│   ├── services/        # Business logic (metrics, alerts, aggregation, etc.)
│   ├── utils/           # Utility classes
│   └── validation/      # Custom validation annotations
├── src/main/resources/
│   └── application.yml  # Application configuration
├── frontend/            # React frontend (Vite + TypeScript)
│   ├── src/             # Frontend source code
│   ├── certs/           # SSL certificates for Nginx
│   ├── nginx.conf       # Nginx configuration
│   └── Dockerfile       # Frontend container build
├── docker-compose.yml   # Full stack orchestration
├── Dockerfile           # Backend container build
├── .env                 # Environment variables (not committed)
└── uploads/             # Device image storage
```
