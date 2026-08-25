# AGENTS.md

eatSmart: Flutter app (`app/`) + Quarkus backend (`backend/`). Stateless, no DB, no auth. Backend proxies receipt photos to AI providers.

## Commands

Backend (run from `backend/`):
- Dev: `./mvnw quarkus:dev` (needs `.env`, see below)
- Build: `./mvnw clean package`
- Tests: `./mvnw test` — **no tests exist yet** in either package

App (run from `app/`):
- `flutter pub get && flutter run`
- Physical device: `flutter run --dart-define=BACKEND_URL=http://<LAN-IP>:8080`
- Android emulator default `http://10.0.2.2:8080` is hardcoded in `lib/api_client.dart`

## Backend architecture (non-obvious)

- Hexagonal: `application/` (use case) → `domain/port/ReceiptAnalysisGateway` ← `infrastructure/` (adapters). Keep new providers behind this port.
- **Two AI providers, chained by `@Priority`**: OpenRouter (`@Priority(1)`, primary) → Gemini (`@Priority(2)`, fallback). Failover happens in `AnalyzeReceiptUseCase`: technical failures fall through to next enabled gateway; a valid "unreadable receipt" answer is never retried.
- Provider is "enabled" iff its API key env var is set: `OPENROUTER_API_KEY` (primary), `GEMINI_API_KEY` (fallback). Both optional individually, but at least one required.
- `.env`: copy `.env.example` → `.env` in `backend/`. Quarkus dev loads it; keys must never be committed or shipped to the app.
- Root README says Gemini-only and `gemini-2.5-flash` — **stale**. Actual config: `openrouter.model=openrouter/free`, `gemini.model=gemini-3.6-flash` in `application.properties`.

## Conventions / gotchas

- Java 25+ (`maven.compiler.release=25`), Quarkus 3.38.3, RESTEasy Reactive.
- All user-facing strings (backend error messages, app UI) are in **Spanish** — keep it that way.
- Max upload: backend accepts 15M (`quarkus.http.limits.max-body-size`), app downscales to 1600px JPEG first.
- App surfaces backend error `message` field verbatim — preserve `ErrorResponse.message` contract in `infrastructure/rest/`.
- No persistence anywhere; don't add accounts/history without explicit request.

## Docs

Detailed per-package docs: `README.md` (root), `backend/README.md`, `app/README.md`. Trust code/config over READMEs when they conflict.

# IMPORTANT (FOLLOW FOR EVERY CHANGE REQUEST)!

Before doing any change ask the user if wants apply the change in a new git worktree or wants to work directly in current branch.

- if new worktree selected: 1º create a new worktree, after changes are done and validated create a new PR with the new changes
- if work directly in the current branch selected: 1º apply changes, validate everything still compiles and tests runs and do not commit or push, leave changes in local.