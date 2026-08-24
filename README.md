# eatSmart

Mobile app that analyzes Spanish supermarket receipts and returns personalized nutrition suggestions in Spanish.

Photograph a receipt, answer a few quick questions, and get an analysis of your purchase: which food groups are missing, how to improve your selection, and how to optimize your budget.

## Architecture

```
eatSmart/
├── app/        # Flutter (Material 3, Spanish UI)
└── backend/    # Quarkus stateless proxy (protects Gemini key)
```

**Flow:** Scan → Profile form → Analyzing → Result (markdown)

**Stack:**
- **Frontend:** Flutter 3.x, Dart, `camera`, `image_picker`, `dio`, `flutter_markdown`
- **Backend:** Quarkus 3.x, Java 25+, RESTEasy Reactive, Google Gemini (`gemini-2.5-flash`, multimodal)
- **Persistence:** None. Stateless, no accounts, no history.

## Requirements

| Component | Version |
|---|---|
| Java | 25+ (verified with OpenJDK 26) |
| Maven | included via `./mvnw` |
| Flutter | stable channel |
| Gemini API Key | [Google AI Studio](https://aistudio.google.com/apikey) |
| Android SDK | emulator or physical device |

## Backend

Stateless proxy: receives receipt photo + user profile, calls Gemini, returns products + suggestions in markdown.

### Run

```bash
cd backend
cp .env.example .env
# edit .env and add your GEMINI_API_KEY
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

**Errors:** `400` unreadable image / invalid request, `502` analysis service failure. Spanish `message` field included.

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

1. **Scan** — Camera or gallery. Preview with confirm/retake.
2. **Profile** — Goal, budget, allergies, diet.
3. **Analyzing** — Spinner with "Analyzing your receipt...".
4. **Result** — Markdown with fixed sections:
   - *General summary*
   - *Missing food groups*
   - *Improvements to your selection*
   - *Budget optimization* (only if `budgetMatters=true`)

## Smoke test

1. Start backend with a valid `GEMINI_API_KEY`.
2. Start app on emulator or device.
3. Photograph a real Spanish supermarket receipt.
4. Fill in the form and tap **Analyze receipt**.
5. Verify: product chips + Spanish sections displayed.
6. Error case: upload blurry photo → Spanish error dialog + retry option.

## Security

- Gemini key only lives in the backend (env `GEMINI_API_KEY`).
- Never in the app or version control.
- `.env.example` documents required variables.

## Technical notes

- App downscales photos to 1600 px (JPEG) before upload.
- Backend timeout: 90 s. App timeout: 120 s.
- No auth, no database, no persistence.
