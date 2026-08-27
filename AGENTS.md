# AGENTS.md

eatSmart: Flutter app (`app/`) + Quarkus backend (`backend/`). Stateless, no DB, no auth. Backend proxies receipt and product photos to AI providers.

## Commands

Backend (run from `backend/`):
- Dev: `./mvnw quarkus:dev` (needs `.env`, see below)
- Build: `./mvnw clean package`
- Tests: `./mvnw test`

App (run from `app/`):
- `flutter pub get && flutter run`
- Physical device: `flutter run --dart-define=BACKEND_URL=http://<LAN-IP>:8080`
- Android emulator default `http://10.0.2.2:8080` is hardcoded in `lib/api_client.dart`

## Backend architecture (non-obvious)

- Hexagonal: `application/` (use cases) → `application/port/` (one driven port per use case: `ReceiptAnalysisGateway`, `ProductAnalysisGateway`, `ShoppingListGenerationGateway`) ← `infrastructure/` (adapters implement all three). Keep new providers behind these ports.
- **Two AI providers, chained by `@Priority`**: OpenRouter (`@Priority(1)`, primary) → Gemini (`@Priority(2)`, fallback). Failover happens in each use case: technical failures fall through to next enabled gateway; a valid business answer ("unreadable receipt" / "not a recognizable product") is never retried.
- **Two independent scan features**, each with its own use case, prompt builder, result parser, and REST endpoint:
  - **Ticket scan**: `POST /api/analyze` → `AnalyzeReceiptUseCase` → returns `{products, suggestions, score}`
  - **Product scan**: `POST /api/analyze/product` → `AnalyzeProductUseCase` → returns `{product, score, nutrition, alternative}`. Alternative is only included when score < 7.
- **Shopping list generation**: `POST /api/shopping-lists/generate` (JSON body, no image) → `GenerateShoppingListUseCase` → returns `{categories: [{name, items: [{name, type, replaces, reason}]}]}`. Uses `ShoppingListGenerationGateway.generateText(prompt)` (text-only port). Categories are fixed in `ShoppingListCategory.ALLOWED_NAMES`; the parser rejects unknown categories, duplicates and invalid REPLACE items. App persists lists locally via `shared_preferences` (`app/lib/data/shopping_list_repository.dart`); backend stays stateless.
- Provider is "enabled" iff its API key env var is set: `OPENROUTER_API_KEY` (primary), `GEMINI_API_KEY` (fallback). Both optional individually, but at least one required.
- `.env`: copy `.env.example` → `.env` in `backend/`. Quarkus dev loads it; keys must never be committed or shipped to the app.
- Root README says Gemini-only and `gemini-2.5-flash` — **stale**. Actual config: `openrouter.models` (ordered fallback list of free vision models; do NOT use `openrouter/free` router — it can pick non-vision models) and `gemini.model=gemini-3.6-flash` in `application.properties`.

## Conventions / gotchas

- Java 25+ (`maven.compiler.release=25`), Quarkus 3.38.3, RESTEasy Reactive.
- All user-facing strings (backend error messages, app UI) are in **Spanish** — keep it that way.
- Max upload: backend accepts 15M (`quarkus.http.limits.max-body-size`), app downscales to 1600px JPEG first.
- App surfaces backend error `message` field verbatim — preserve `ErrorResponse.message` contract in `infrastructure/rest/`.
- No persistence anywhere; don't add accounts/history without explicit request.
- Product scan alternative threshold: score < 7 triggers alternative suggestion (enforced in `AnalyzeProductUseCase`).

## Docs

Detailed per-package docs: `README.md` (root), `backend/README.md`, `app/README.md`. Trust code/config over READMEs when they conflict.

# IMPORTANT: If you find errors related to files not found in the current working project directory (example: .env file), look for them in ~/Projects/eatSmartAI directory.