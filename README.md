# Project-Success (College Bus Tracking System)

Monorepo for the Bronzera Labs College Bus Tracking System.

## Prerequisites
- Java 17+
- Docker & `docker-compose`
- Node.js 18+

## 1. Environment Setup
Copy `.env.example` to `.env` and fill in your local values:

    cp .env.example .env

## 2. Start Postgres & Redis
From the repo root (`Project-Success/`):

    sudo docker-compose up -d

- PostgreSQL 16 runs on `localhost:5433` (`mybus`)
- Redis 7 runs on `localhost:6379`

## 3. Run the Backend

    cd Backend
    export JWT_SECRET=$(openssl rand -base64 48)
    SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

- **Local profile (`local`):** When `spring.mail.username` is blank, student 6-digit OTP codes are printed in the backend log (`[LOCAL OTP] email -> code`).
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

## Folder Map
- `Backend/` — Spring Boot 4 / Java 17 REST API + Flyway migrations
- `My Bus Tracker/` — Driver mobile app (React Native + TypeScript)
- `My College Bus/` — Student mobile app (React Native + TypeScript)
- `My Bus Admin/` — College transport admin web portal (React + Vite)
- `Bronzera Admin Control/` — Bronzera Labs super-admin portal (React + Vite)
- `Documents/` — Requirements & reference docs