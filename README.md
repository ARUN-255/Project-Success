# Project-Success (College Bus Tracking System)

## Prerequisites
- Java 17+, Docker & `docker-compose`, Node.js 18+

## 1. Environment Variables
Copy `.env.example` to `.env` and fill in your local values:
```bash
cp.env.example .env
export JWT_SECRET=$(openssl rand -base64 48)