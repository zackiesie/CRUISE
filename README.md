# CRUISE Car Rental System

CRUISE is a practice full-stack car rental application with a JavaFX desktop client and a Spring Boot REST API.

## New teammate / GitHub setup

- **Windows after cloning:** follow [SETUP-WINDOWS.md](SETUP-WINDOWS.md).
- **First GitHub upload:** follow [GITHUB-UPLOAD.md](GITHUB-UPLOAD.md).
- Copy `.env.example` to `.env`, fill it in locally, then use `scripts/run.ps1` to load the configuration and run the backend/client.
- Cloning copies code. Each laptop needs its own local database and configuration; uploaded vehicle images and existing bookings are not included.

## Features

- Customer car browsing with search/filter, vehicle details and backend-hosted images
- Google sign-in for customers (Desktop OAuth + backend session token)
- Date-based booking and overlap checks
- Stripe Checkout test payments, webhook updates and customer cancellation/refund
- Automatic expiry of unpaid reservations
- Separate admin login/session
- Admin dashboard, car CRUD, image upload and booking management view
- MySQL persistence through Spring Data JPA

## Project structure

```text
CRUISE/
├── simpleapi/       Spring Boot API
├── simpleclient/    JavaFX desktop client
├── .env.example     environment-variable reference only
└── README.md
```

## Requirements

- JDK 21 or newer
- Maven 3.9+
- MySQL 8+
- Stripe CLI for local webhook forwarding
- A Stripe test-mode account
- Google OAuth credentials

> The backend may start on older MySQL versions, but MySQL 8+ is recommended for the current Hibernate stack.

## 1. Database

Create the database:

```sql
CREATE DATABASE simpleapi_db;
```

The default local configuration expects `root` with a blank password. For another setup, define `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.

## 2. Environment variables

Do **not** commit real credentials. Configure these in your OS/IDE:

```text
DB_URL                         optional; defaults to jdbc:mysql://localhost:3306/simpleapi_db
DB_USERNAME                    optional; defaults to root
DB_PASSWORD                    optional; defaults to blank
STRIPE_SECRET_KEY              required
STRIPE_WEBHOOK_SECRET          required
GOOGLE_CLIENT_ID               required by Spring Google OAuth
GOOGLE_CLIENT_SECRET           required by Spring Google OAuth
GOOGLE_DESKTOP_CLIENT_ID       required by JavaFX Google login
GOOGLE_DESKTOP_CLIENT_SECRET   required by the current desktop OAuth implementation
CRUISE_ADMIN_USERNAME          required to create the initial admin
CRUISE_ADMIN_PASSWORD          required to create the initial admin
```

On Windows PowerShell, persistent variables can be created with `setx`, for example:

```powershell
setx CRUISE_ADMIN_USERNAME "admin"
setx CRUISE_ADMIN_PASSWORD "choose-a-strong-password"
```

Restart NetBeans/your terminal after changing persistent environment variables.

The initial admin is created only when that username does not already exist. Changing the environment password later does not automatically change an existing database admin password.

## 3. Start the API

```bash
cd simpleapi
mvn spring-boot:run
```

The API runs at `http://localhost:8080`.

## 4. Stripe webhook (local development)

In another terminal:

```bash
stripe login
stripe listen --events checkout.session.completed,checkout.session.expired --forward-to localhost:8080/api/stripe/webhook
```

Set `STRIPE_WEBHOOK_SECRET` to the `whsec_...` value printed by Stripe CLI, then restart the API if the value changed.

Use Stripe **test mode** credentials while developing.

## 5. Google OAuth

Configure Google OAuth credentials for both flows used by this project:

- Spring/browser OAuth: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- JavaFX desktop OAuth: `GOOGLE_DESKTOP_CLIENT_ID`, `GOOGLE_DESKTOP_CLIENT_SECRET`

The desktop login opens the system browser and returns to a temporary local callback listener created by the JavaFX app.

## 6. Start the JavaFX client

```bash
cd simpleclient
mvn javafx:run
```

The application starts in the customer interface. Use **ADMIN** in the top navigation to open the separate admin login.

## Local development flow

1. Start MySQL.
2. Start `simpleapi`.
3. Start Stripe CLI webhook forwarding.
4. Start `simpleclient`.
5. Customer: browse → Google login → choose dates → Stripe Checkout → My Bookings.
6. Admin: Admin Console → sign in → Dashboard / Car Management / Bookings.

## Security / repository notes

- No real API keys, OAuth secrets or admin passwords belong in Git.
- `.env` files, Maven `target/`, NetBeans private settings and runtime vehicle uploads are ignored.
- Customer and admin raw session tokens are generated randomly; the backend stores token hashes.
- Passwords for admin accounts are stored as BCrypt hashes.
- Admin API controllers validate the custom admin Bearer token.
- JavaFX currently stores app session tokens with Java Preferences. This is suitable for this practice project, but a production desktop app should use an OS-backed secure credential store.
- Uploaded car images are runtime data and are not included in the repository.

## Before pushing to GitHub

Run both builds locally:

```bash
cd simpleapi
mvn clean test

cd ../simpleclient
mvn clean package
```

Then inspect what Git will commit:

```bash
git status
git diff --cached
```

Never push a secret that was committed previously. If a real secret ever entered Git history, rotate/revoke it before publishing the repository.

## License

No license has been selected. Add a license file if you intend others to reuse or redistribute the project.

## UX polish

The customer experience includes a clearer three-step rental journey and live payment confirmation. After Stripe checkout opens, the JavaFX client checks the authenticated booking status and shows an animated confirmation only after the backend reports the booking as paid. This keeps the UI confirmation tied to the webhook-confirmed server state.
