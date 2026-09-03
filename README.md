# eatSmart

Mobile app that analyzes Spanish supermarket receipts and returns personalized nutrition suggestions in Spanish.

Photograph a receipt, answer a few quick questions, and get an analysis of your purchase: which food groups are missing, how to improve your selection, and how to optimize your budget.

## Architecture

```
eatSmart/
├── app/        # Flutter (Material 3, Spanish UI)
└── backend/    # Quarkus stateless proxy (protects Gemini key)
```

**Flow:** Scan (ticket or product) → Profile form → Analyzing → Result (markdown) → optional shopping list generation

**Stack:**
- **Frontend:** Flutter 3.x, Dart, `camera`, `image_picker`, `dio`, `flutter_markdown`, `google_mobile_ads`, `shared_preferences`
- **Backend:** Quarkus 3.x, Java 25+, RESTEasy Reactive. Two chained AI providers: OpenRouter (primary) → Gemini (fallback)
- **Persistence:** None on the backend. The app persists credits and shopping lists locally (`shared_preferences`).

**Monetization:** AdMob banners (top of scan and shopping list screens) + rewarded video ads that grant scan/generation credits. See [app README](app/README.md#monetization-admob--credits) for the credit system.

## Requirements

| Component | Version |
|---|---|
| Java | 25+ (verified with OpenJDK 26) |
| Maven | included via `./mvnw` |
| Flutter | stable channel |
| Gemini API Key | [Google AI Studio](https://aistudio.google.com/apikey) |
| Android SDK | emulator or physical device |

## Backend

Stateless proxy: receives photo + user profile, chains OpenRouter → Gemini, returns products + suggestions in markdown. Providers are enabled by their API key env var (`OPENROUTER_API_KEY`, `GEMINI_API_KEY`); at least one required.

### Run

```bash
cd backend
cp .env.example .env
# edit .env and add OPENROUTER_API_KEY and/or GEMINI_API_KEY
./mvnw quarkus:dev
```

Listens on `http://localhost:8080`.

### API

**`POST /api/analyze`** — `multipart/form-data`

| Field | Type | Description |
|---|---|---|
| `image` | file | Receipt photo (JPEG/PNG, max 10 MB) |
| `goal` | text | `LOSE` · `MAINTAIN` · `GAIN` |
| `budgetMatters` | text | `true` · `false` |
| `allergies` | text | Free text (can be empty) |
| `dietPreference` | text | `NONE` · `VEGETARIAN` · `VEGAN` · `OTHER` |

**200 Response:**

```json
{
  "products": ["whole milk", "white bread"],
  "suggestions": "## General summary\n..."
}
```

**Errors:** `400` unreadable image / invalid request, `502` analysis service failure. Spanish `message` field included. `502` messages are generic ("No se pudo completar el análisis...") — provider names and details only appear in server logs, never in API responses.

### curl example

```bash
curl -X POST http://localhost:8080/api/analyze \
  -F "image=@ticket.jpg;type=image/jpeg" \
  -F "goal=MAINTAIN" \
  -F "budgetMatters=true" \
  -F "allergies=lactose" \
  -F "dietPreference=NONE"
```

## App (Flutter)

### Run

```bash
cd app
flutter pub get
flutter run
```

**Android emulator:** uses `http://10.0.2.2:8080` (default).

**Physical device:**

```bash
flutter run --dart-define=BACKEND_URL=http://192.168.1.50:8080
```

Backend URL is `kBackendBaseUrl` in `lib/api_client.dart`.

### User flow

1. **Scan** — Camera or gallery (ticket or product tab). Preview with confirm/retake. Costs 1 credit per completed analysis.
2. **Profile** — Goal, budget, allergies, diet.
3. **Analyzing** — Spinner with "Analyzing your receipt/product...".
4. **Result** — Markdown with fixed sections:
   - *General summary*
   - *Missing food groups*
   - *Improvements to your selection*
   - *Budget optimization* (only if `budgetMatters=true`)
5. **Shopping list** (ticket results only) — Generate a suggested shopping list (1 credit), saved locally.

Credits: the AppBar shows `Créditos: N`; the **+** button plays a rewarded video ad granting credits (default 3 per video, new users start with 1). Credits are only spent when the analysis/generation actually produces an answer — technical failures are free.

## Smoke test

1. Start backend with a valid `GEMINI_API_KEY`.
2. Start app on emulator or device.
3. Photograph a real Spanish supermarket receipt.
4. Fill in the form and tap **Analyze receipt**.
5. Verify: product chips + Spanish sections displayed.
6. Error case: upload blurry photo → Spanish error dialog + retry option.

## Security

- AI keys only live in the backend (env `OPENROUTER_API_KEY` / `GEMINI_API_KEY`).
- AdMob app/unit IDs live in the app (public by design); API keys never in the app or version control.
- `.env.example` documents required variables.

## Technical notes

- App downscales photos to 1600 px (JPEG) before upload.
- Backend timeout: 90 s. App timeout: 120 s.
- No auth, no database, no persistence.
