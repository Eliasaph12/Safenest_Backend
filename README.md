# SafeNest Backend

SafeNest Backend is a Spring Boot 3 API for authentication, OTP verification, chat support, admin activity logs, legal/counselling workflows, and MySQL-backed persistence.

## Stack

- Java 21
- Spring Boot 3.2
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Boot Actuator
- MySQL

## Run Locally

From `SafeNest-backend`:

```powershell
mvn spring-boot:run
```

The backend starts on `http://localhost:8080` by default.

## Frontend API Base URL

Set the frontend to use:

```text
VITE_API_URL=http://localhost:8080
```

## Environment Variables

Use `.env` locally or set the same variables in your host platform:

```text
PORT=8080
DB_HOST=localhost
DB_PORT=3306
DB_NAME=safenest_db
DB_USER=safenest_user
DB_PASSWORD=your_secure_password
FRONTEND_URL=http://localhost:5173
OTP_DEV_MODE=true
OTP_EXPIRY_MINUTES=10
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
OPENAI_API_KEY=
OPENAI_MODEL=gpt-5
```

## Render Deployment

This repository now includes [render.yaml](C:\Users\Admin\Desktop\FSAD Project\SafeNest-backend\render.yaml) for Render.

Use these settings if you create the service manually:

- Runtime: `Java`
- Build Command: `mvn clean package -DskipTests`
- Start Command: `java -jar target/backend-0.0.1-SNAPSHOT.jar`
- Health Check Path: `/api/auth/health`

Set these environment variables in Render:

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASSWORD`
- `FRONTEND_URL`
- `OTP_DEV_MODE`
- `OTP_EXPIRY_MINUTES`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_SMTP_AUTH`
- `MAIL_SMTP_STARTTLS`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`

Important:
- Render provides its own `PORT`, and the backend now respects it.
- `FRONTEND_URL` can be a comma-separated list of allowed origins if you deploy multiple frontend URLs.
- Your database must be reachable from Render. If you are using a local MySQL instance on your laptop, Render will not be able to access it.

## Available Routes

- `POST /api/auth/login`
- `POST /api/auth/login/verify-otp`
- `POST /api/auth/signup`
- `POST /api/auth/signup/verify-otp`
- `GET /api/auth/health`
- `GET /api/products`
- `POST /api/products`
- `PUT /api/products/{id}`
- `DELETE /api/products/{id}`
- `GET /api/appointments/counsellor/{id}`
- `GET /api/appointments/victim/{id}`
- `POST /api/appointments`
- `DELETE /api/appointments/{id}`
- `GET /api/casenotes/counsellor/{id}`
- `GET /api/casenotes/victim/{id}`
- `POST /api/casenotes`
- `DELETE /api/casenotes/{id}`
- `GET /api/legalcases/advisor/{id}`
- `GET /api/legalcases/victim/{id}`
- `POST /api/legalcases`
- `DELETE /api/legalcases/{id}`
- `GET /api/chat/agent`
- `GET /api/chat/messages`
- `POST /api/chat/messages`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/stats`
- `GET /api/admin/activities`

## Notes

- Core app data is MySQL-backed.
- Spring Boot Actuator health is available at `/actuator/health`.
- A frontend-friendly health endpoint is also available at `/api/auth/health`.
