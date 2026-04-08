# E-Commerce Backend

This is the backend for the E-Commerce application, built with Spring Boot.

## Prerequisites

- Java 21 (or 17+)
- Maven 3.8+

## Local Setup

1. Install Java 21, Maven.
2. Copy `.env.example` to `.env` and update DB creds (or create local MySQL `ecommerce_db`).
3. `mvn clean install`
4. `mvn spring-boot:run` (runs on port 8080)

**Note:** Uses H2 in-memory for tests, MySQL via env vars for prod/local.

## Deployment to Railway

1. Push changes to GitHub repo.
2. railway.app: New Project → Deploy from GitHub repo.
3. Add MySQL plugin (or use existing).
4. Set Environment Variables (from Railway DB dashboard):
   - `DATABASE_URL` (JDBC URL)
   - `DATABASE_USER`
   - `DATABASE_PASSWORD`
   - `SERVER_PORT=8080`
   - `SPRING_PROFILES_ACTIVE=prod`
   - `FRONTEND_URL=https://your-frontend.railway.app` (for CORS)
5. Deploy! Procfile auto-used, Flyway migrates DB.

Monitor logs, health: `/actuator/health`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/signup` | Register user |
| POST | `/api/auth/login` | Login user |
| GET | `/api/products` | List products |
| POST | `/api/products` | Create product |
| GET | `/api/products/{id}` | Get product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

**CORS:** Configurable via `FRONTEND_URL` env (default: http://localhost:3000).

See `DATABASE_SETUP.md` for DB details, `TODO.md` for progress.
