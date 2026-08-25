# eatSmartAI App

Flutter mobile app that photographs Spanish supermarket receipts, sends them to the eatSmart backend for AI analysis, and displays personalized nutrition suggestions in Spanish.

## Features

- Camera capture or gallery import of receipts
- Profile form for health goals, budget, allergies, and diet preferences
- Real-time analysis with loading indicator
- Markdown-rendered results with product identification and nutrition advice
- Spanish UI throughout

## Tech Stack

| Package | Purpose |
|---|---|
| `camera` | Receipt photo capture |
| `image_picker` | Gallery fallback |
| `dio` | HTTP multipart upload |
| `flutter_markdown` | Render analysis results |
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

**Physical device:**

```bash
flutter run --dart-define=BACKEND_URL=http://192.168.1.X:8080
```

Backend URL is configured in `lib/api_client.dart` (`kBackendBaseUrl`).

## Project Structure

```
lib/
├── main.dart              # App entry, routes to ScanScreen
├── api_client.dart        # Dio client, AnalysisResult model
├── image_utils.dart       # downscaleImage() - compresses to 1600px JPEG
└── screens/
    ├── scan_screen.dart   # Camera/gallery capture + preview
    ├── form_screen.dart   # Profile form (goal, budget, allergies, diet)
    ├── analysis_screen.dart # Loading spinner during API call
    └── result_screen.dart # Markdown results + product chips
```

## User Flow

1. **Scan** - Capture receipt via camera or select from gallery
2. **Profile** - Enter goal (lose/maintain/gain), budget preference, allergies, diet
3. **Analyzing** - Loading screen while backend processes receipt
4. **Result** - View product identification and nutrition suggestions in markdown

## Build

```bash
# Android
flutter build apk

# iOS
flutter build ios

# Web
flutter build web
```

## Backend

This app requires the eatSmart backend to function. See the [project README](../README.md) for backend setup, API documentation, and security details.
