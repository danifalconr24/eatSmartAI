# eatSmartAI App

Flutter mobile app that photographs Spanish supermarket receipts, sends them to the eatSmart backend for AI analysis, and displays personalized nutrition suggestions in Spanish.

## Features

- Home with floating bottom navigation: Ticket scan, Product scan, Shopping lists
- Camera capture or gallery import of receipts and products
- Profile form for health goals, budget, allergies, and diet preferences
- Real-time analysis with loading indicator
- Markdown-rendered results with product identification and nutrition advice
- Suggested shopping lists persisted locally (`shared_preferences`)
- Nutritionist chat ("Chat" button in the result screens' AppBar) opens as a floating popup over the results; the session is kept while the result screen stays open, so closing and reopening the popup keeps the history. Limited to **2 questions per analysis** — the popup header shows remaining questions and the input disables when exhausted
- Monetization: AdMob banners + rewarded video ads with a credit system (see below)
- Portrait-only, Spanish UI throughout

## Monetization (AdMob + credits)

The app is monetized with Google AdMob:

- **Banner ads** — shown at the top of both scan screens (right above the camera frame) and at the top of the shopping lists screen (right below the app bar). Implemented in `lib/widgets/banner_ad_widget.dart`; shows an "Ad" placeholder while loading (debug/preview only, disable with `--dart-define=SHOW_AD_PLACEHOLDER=false`).
- **Rewarded video ads → credits** — the AppBar shows a `Créditos: N` chip with a **+** button that plays a rewarded video and grants credits.

### Credit system

`lib/ads/credit_service.dart` (singleton `ChangeNotifier`, persisted with `shared_preferences`):

- **1 credit =** 1 receipt/product scan **or** 1 shopping list generation.
- New users start with `INITIAL_CREDITS` (default **1**).
- Each fully watched rewarded video grants the amount configured in the **AdMob rewarded ad unit** (reward amount). If AdMob returns `0` or an invalid amount, the app falls back to `CREDITS_PER_REWARD` (default **3**).

```bash
flutter run \
  --dart-define=CREDITS_PER_REWARD=5 \
  --dart-define=INITIAL_CREDITS=2
```

### When credits are charged

Charged **only when the request produces an answer** — checked at submit, spent after the response:

| Outcome | HTTP | Credit |
|---|---|---|
| Analysis / list generation success | 200 | −1 |
| "Unreadable receipt" / "product not recognizable" (valid business answer) | 400 | −1 |
| Technical failure (provider down, network error) | 502 / network | 0 |

With 0 credits, a dialog offers watching a video inline; earning credits retries the action automatically.

## Tech Stack

| Package | Purpose |
|---|---|
| `camera` | Receipt photo capture |
| `image_picker` | Gallery fallback |
| `dio` | HTTP multipart upload |
| `flutter_markdown` | Render analysis results |
| `google_mobile_ads` | AdMob banners + rewarded ads |
| `shared_preferences` | Credits balance + shopping lists |
| `image` | Downscale/compress photos |
| `path_provider` | Temp directory access |

**Flutter:** stable channel | **Dart:** ^3.11.1

## Prerequisites

- Flutter SDK (stable channel)
- Android SDK (emulator or physical device)
- Backend running ([setup instructions](../README.md#backend))

## Getting Started

```bash
flutter pub get
flutter run
```

**Android emulator** (default): uses `http://10.0.2.2:8080` for backend.

**iOS simulator:** shares the host network, so `localhost` reaches the backend directly.

```bash
flutter run --dart-define=BACKEND_URL=http://localhost:8080
```

**Physical device:**

```bash
flutter run --dart-define=BACKEND_URL=http://192.168.1.X:8080
```

Backend URL is configured in `lib/api_client.dart` (`kBackendBaseUrl`).

## Project Structure

```
lib/
├── main.dart                    # App entry, portrait lock, ads + credits init
├── api_client.dart              # Dio client; 400 → UnreadableImageException
├── image_utils.dart             # downscaleImage() - compresses to 1600px JPEG
├── shared_camera.dart           # Shared CameraController across scan tabs
├── ads/
│   ├── ad_service.dart          # AdMob config, banner load, rewarded show
│   └── credit_service.dart      # Credit balance (shared_preferences)
├── widgets/
│   ├── banner_ad_widget.dart    # Top banner (with debug placeholder)
│   ├── credits_chip.dart        # AppBar "Créditos: N" + rewarded button
│   ├── credits_dialog.dart      # Out-of-credits dialog (watch video inline)
│   └── floating_nav_space.dart  # Runtime-measured floating nav bar height
└── screens/
    ├── home_screen.dart         # PageView + floating bottom nav bar
    ├── scan_screen.dart         # Camera/gallery capture (ticket & product)
    ├── form_screen.dart         # Profile form; checks credits before scan
    ├── analysis_screen.dart     # Ticket loading; spends credit on result
    ├── product_analysis_screen.dart  # Product loading; spends credit on result
    ├── result_screen.dart       # Ticket results; list generation spends credit
    ├── product_result_screen.dart    # Product results
    └── shopping_lists_screen.dart    # Saved lists + top banner
```

## User Flow

1. **Scan** - Capture receipt/product via camera or select from gallery
2. **Profile** - Enter goal (lose/maintain/gain), budget preference, allergies, diet
3. **Analyzing** - Loading screen while backend processes the image
4. **Result** - Product identification + nutrition suggestions in markdown; optional shopping list generation (1 credit)

## Responsive design

- Portrait only (`SystemChrome` + iOS `Info.plist`).
- The floating bottom nav bar is measured at runtime (`FloatingNavSpace`) so embedded screens always clear it on any device/inset.
- Fixed-size widgets (score header, credits chip, chat bubbles, ad placeholder) clamp/scale for narrow screens and large system font sizes.

## Build

```bash
# Android
flutter build apk

# iOS
flutter build ios

# Web
flutter build web
```

## Configuration overrides

All runtime overrides are applied via `--dart-define=KEY=value`:

| Key | Type | Default | Description |
|---|---|---|---|
| `BACKEND_URL` | `String` | `http://10.0.2.2:8080` | Base URL of the eatSmart backend. Use `http://localhost:8080` for iOS simulator or `http://<LAN-IP>:8080` for physical devices. |
| `SHOW_AD_PLACEHOLDER` | `bool` | `true` | Shows a debug "Ad" placeholder while a banner is loading or when it fails. Set to `false` to hide the placeholder (release builds already hide it on failure). |
| `CREDITS_PER_REWARD` | `int` | `3` | Fallback credits granted for each rewarded video, used only when the AdMob ad unit reward amount is missing or `0`. Set the real value in the AdMob console. |
| `INITIAL_CREDITS` | `int` | `1` | Starting credit balance for new users (first launch only). |

Example combining all overrides:

```bash
flutter run \
  --dart-define=BACKEND_URL=http://192.168.1.42:8080 \
  --dart-define=SHOW_AD_PLACEHOLDER=false \
  --dart-define=CREDITS_PER_REWARD=5 \
  --dart-define=INITIAL_CREDITS=2
```

### Hard-coded AdMob IDs

Production AdMob IDs live in code (not configurable via `--dart-define`):

- Banner + rewarded ad units: `lib/ads/ad_service.dart`
- Android app ID: `android/app/src/main/AndroidManifest.xml`
- iOS app ID: `ios/Runner/Info.plist`

Update those files directly when AdMob console generates new IDs.

## Backend

This app requires the eatSmart backend to function. See the [project README](../README.md) for backend setup, API documentation, and security details.
