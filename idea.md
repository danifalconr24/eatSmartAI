# eatSmart — MVP Development Prompt

## Mission

Build an MVP of **eatSmart**, a mobile app that analyzes supermarket receipts and gives personalized nutrition suggestions.

The user photographs a supermarket receipt with the phone camera. An AI agent (multimodal LLM) reads the purchased products from the photo, asks the user a few quick profile questions via a form, and returns actionable suggestions: what food groups are missing, how to improve the product selection, and how to optimize the list for budget and health goals.

Target market: Spain. All UI text, LLM prompts, and suggestions must be in **Spanish**. Receipts come from Spanish supermarkets (Mercadona, Carrefour, Lidl, Dia, Alcampo, etc.).

## User Flow

1. **Scan screen** — User opens the app and takes a photo of a supermarket receipt (or picks one from the gallery). Show a preview with confirm/retake.
2. **Profile form** — After confirming the photo, show a one-screen form:
   - **Goal** (required, segmented picker): `Perder peso` / `Mantenerme` / `Ganar peso`
   - **Budget matters** (required, yes/no toggle): "¿Te importa el presupuesto?"
   - **Allergies / intolerances** (optional, free text): e.g. "lactosa, gluten"
   - **Dietary preference** (required, picker): `Sin preferencia` / `Vegetariano` / `Vegano` / `Otra`
3. **Analysis** — App sends the photo + form answers to the backend. Show a loading state ("Analizando tu ticket...").
4. **Result screen** — Rendered markdown with **fixed sections**:
   - `Resumen general` — short overview of the purchase (healthiness, balance)
   - `Grupos de alimentos que faltan` — missing categories (fish, legumes, fruit, vegetables, whole grains, etc.)
   - `Mejoras en tu selección` — concrete product swaps (e.g. "white bread → whole grain"), referencing actual products detected on the receipt
   - `Optimización de presupuesto` — **only if** the user said budget matters: cheaper alternatives, brand swaps, bulk-buy tips
5. User can go back and scan a new receipt (stateless — no history).

## Architecture

Two components in one monorepo:

```
eatSmart/
├── app/        # Flutter mobile app
└── backend/    # Quarkus proxy service
```

### Backend — Quarkus (Java 25)

Thin, stateless proxy that protects the LLM API key.

- **Quarkus + Java 25**, REST endpoint(s) via Quarkus REST (Jakarta REST / resteasy-reactive-jackson).
- Single endpoint, e.g. `POST /api/analyze`:
  - **Request**: `multipart/form-data` (or JSON with base64 image — pick one and document):
    - `image`: receipt photo (JPEG)
    - `goal`: `LOSE | MAINTAIN | GAIN`
    - `budgetMatters`: boolean
    - `allergies`: string (may be empty)
    - `dietPreference`: `NONE | VEGETARIAN | VEGAN | OTHER`
  - **Response** `200 OK`:
    ```json
    {
      "products": ["leche entera", "pan blanco", "..."],
      "suggestions": "## Resumen general\n...markdown with the fixed sections..."
    }
    ```
  - Errors: `400` unreadable/invalid image, `502` LLM upstream failure. Return a Spanish user-friendly `message` field.
- Calls **Google Gemini** (use `gemini-2.5-flash` or current Flash model — fast + vision-capable) with the receipt image inline. API key from environment variable `GEMINI_API_KEY` (Quarkus config property, never hardcoded, add to `.gitignore` / use `.env.example`).
- Construct a system/user prompt (in Spanish) that instructs Gemini to:
  1. Extract the product list from the receipt (ignore prices-only noise, loyalty lines, totals; keep product names).
  2. Analyze against the user's goal, allergies, diet preference, and budget flag.
  3. Return the fixed markdown sections exactly as defined in the User Flow. Respect allergies strictly (never suggest an allergen). Respect diet preference strictly.
  4. If the image is not a readable supermarket receipt, return a clear Spanish error message.
- Request timeout + basic logging. No database, no auth, no persistence.

### App — Flutter

- **Flutter** (stable channel), Dart, Material 3, Spanish UI.
- Screens: Scan → Form → Loading → Result (simple `Navigator` push flow; no complex state management needed — `setState`/`ValueNotifier` or Riverpod if preferred, keep it simple).
- Camera via `camera` plugin (photo capture) + `image_picker` for gallery fallback. Downscale/compress image before upload (max ~1600px long edge) to keep payload reasonable.
- HTTP via `dio` or `http` (multipart). Backend base URL configurable in one constant (default `http://10.0.2.2:8080` for Android emulator).
- Result screen renders markdown (`flutter_markdown`). Error dialog with retry on failure.
- No accounts, no local persistence, no analytics, no push notifications.

## Out of Scope (explicitly NOT in MVP)

- User accounts / auth / cloud sync
- Receipt history or any persistence
- Price tracking, product barcode lookup, external nutrition databases
- Multi-photo / multi-receipt scans
- Internationalization beyond Spanish
- App store / TestFlight distribution, CI/CD
- Unit/widget/integration test suites (manual smoke-testing is enough for MVP)

## Definition of Done

1. `cd backend && ./mvnw quarkus:dev` (or `./gradlew quarkusDev` if Gradle) starts successfully with `GEMINI_API_KEY` set.
2. `cd app && flutter pub get && flutter run` compiles and launches on Android emulator.
3. Manual end-to-end smoke test documented in README: real Spanish supermarket receipt photo → form → result screen shows all expected sections in Spanish.
4. `README.md` at repo root: setup steps (Java 25, Flutter, Gemini API key), how to run backend + app, one example curl against `POST /api/analyze`.
5. Clean error path: blurry/non-receipt photo produces a Spanish error message in-app, not a crash.

## Working Agreements for the Agent

- Prefer simple, idiomatic code over clever abstractions. This is an MVP.
- Follow each framework's standard project layout (`flutter create`, Quarkus starter).
- Do not add dependencies beyond what is listed or clearly necessary.
- Keep secrets out of version control; provide `.env.example` / config placeholders.
- Verify each Definition of Done item before finishing; fix issues found during verification.
