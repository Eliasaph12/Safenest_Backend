# SafeNest Backend

This backend now runs as a Spring Boot 3 application and serves the API expected by the SafeNest frontend.

## Stack

- Java 21
- Spring Boot 3.2
- Spring Web
- Spring Security
- Spring Boot Actuator

## Run Locally

From `SafeNest-backend`:

```powershell
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`.

## Frontend API Base URL

Set the frontend to use:

```text
VITE_API_URL=http://localhost:8080
```

## Available Routes

- `POST /api/auth/login`
- `POST /api/auth/signup`
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
- `GET /api/chat/messages`
- `POST /api/chat/messages`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `DELETE /api/admin/users/{id}`
- `GET /api/admin/stats`

## Notes

- The current implementation uses seeded in-memory data so the frontend can run without database setup.
- Spring Boot Actuator health is available at `/actuator/health`.
- A frontend-friendly health endpoint is also available at `/api/auth/health`.
