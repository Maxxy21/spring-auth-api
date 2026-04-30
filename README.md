# Spring Auth API

A secure user registration and authentication backend built with Java and Spring Boot .

## Features

- **Secure Registration** — BCrypt password hashing, duplicate email detection, Google reCAPTCHA v2
- **Email Verification** — UUID token sent on registration; 24-hour expiry
- **Brute-Force Protection** — tracks failed login attempts per account; auto-locks after 5 failures for 30 minutes
- **JWT Authentication** — short-lived access tokens (15 min) with refresh token rotation (7 days)
- **Multi-Factor Authentication** — TOTP via Google Authenticator; enable/disable per user
- **RFC 7807 Error Responses** — structured `ProblemDetail` JSON on all errors

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2, Spring Security 6 |
| Database | PostgreSQL + Flyway migrations |
| Auth | JWT (jjwt), BCrypt |
| MFA | TOTP (GoogleAuth) |
| Testing | JUnit 5, Mockito, MockMvc |

## API Endpoints

### Auth (`/api/auth`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/register` | Register new account (CAPTCHA required) |
| GET | `/verify-email?token=` | Verify email address |
| POST | `/resend-verification` | Resend verification email |
| POST | `/login` | Authenticate; returns tokens or MFA challenge |
| POST | `/mfa/validate` | Complete MFA login with TOTP code |
| POST | `/refresh` | Rotate refresh token, get new access token |
| POST | `/logout` | Revoke refresh token |

### Users (`/api/users`) — requires JWT

| Method | Path | Description |
|--------|------|-------------|
| GET | `/profile` | Get current user profile |
| POST | `/mfa/setup` | Generate TOTP secret + QR code URL |
| POST | `/mfa/enable` | Confirm and enable MFA |
| POST | `/mfa/disable` | Disable MFA (requires current TOTP code) |

## Local Setup

**Prerequisites:** JDK 17, Maven 3.8+, Docker

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts PostgreSQL on port 5432 and MailHog (SMTP + web UI) on port 1025/8025.

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env — set JWT_SECRET and mail credentials at minimum
```

The app reads all secrets from environment variables (loaded automatically from `.env` via spring-dotenv). `JWT_SECRET` has no fallback and the app will refuse to start without it.

### 3. Run the application

```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`. Flyway runs migrations automatically on startup.

### 4. Key environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JWT_SECRET` | **yes** | HS256 signing key — min 32 characters |
| `DB_URL` | no | Defaults to `jdbc:postgresql://localhost:5432/user_registration` |
| `DB_USERNAME` | no | Defaults to `postgres` |
| `DB_PASSWORD` | no | Defaults to `postgres` |
| `MAIL_USERNAME` | yes (for email) | SMTP username |
| `MAIL_PASSWORD` | yes (for email) | SMTP app password |
| `CAPTCHA_ENABLED` | no | Set to `false` to skip CAPTCHA locally |
| `CAPTCHA_SECRET_KEY` | no | Google reCAPTCHA v2 secret (defaults to test key) |

### 5. Run tests

```bash
mvn test
```

Tests use an in-memory H2 database and disable CAPTCHA automatically via the `test` profile.

## MFA Flow

```
POST /api/auth/login
  → { mfaRequired: true, tempToken: "..." }

POST /api/auth/mfa/validate  { tempToken, totpCode }
  → { accessToken, refreshToken }
```

## Security Notes

- Passwords are never returned in any response
- Error messages for invalid credentials are intentionally generic to prevent user enumeration
- Refresh token reuse detection: if a revoked token is presented, all tokens for that user are revoked
