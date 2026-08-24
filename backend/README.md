# EatSmart Backend

Backend API for receipt analysis and food suggestions using AI. Built with Quarkus.

## Overview

EatSmart analyzes grocery receipts via AI to provide:
- Product extraction from receipt images
- Personalized food suggestions based on goals, diet, and allergies

## Architecture

```
src/main/java/com/eatsmart/
├── application/        # Use cases and business logic
│   ├── AnalyzeReceiptUseCase.java
│   ├── AnalysisResultParser.java
│   └── ReceiptPromptBuilder.java
├── domain/            # Domain models and ports
│   ├── model/
│   └── port/
└── infrastructure/    # External integrations
    ├── rest/          # REST API endpoints
    ├── openrouter/    # OpenRouter AI client
    └── gemini/        # Gemini AI client (fallback)
```

## API Endpoint

### `POST /api/analyze`

Analyzes a receipt image and returns product suggestions.

**Request (multipart/form-data):**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| image | file | Yes | Receipt image (max 10MB) |
| goal | string | Yes | `LOSE`, `MAINTAIN`, or `GAIN` |
| dietPreference | string | No | `NONE`, `VEGETARIAN`, `VEGAN`, or `OTHER` |
| budgetMatters | boolean | No | Consider budget in suggestions |
| allergies | string | No | Comma-separated allergies |

**Response (200):**
```json
{
  "products": ["milk", "bread", "eggs"],
  "suggestions": "Based on your LOSE goal..."
}
```

## Prerequisites

- Java 25+
- Maven 3.9+

## Environment Variables

```bash
# Required: OpenRouter API key
OPENROUTER_API_KEY=your-key-here

# Optional: Gemini API key (fallback)
GEMINI_API_KEY=your-key-here
```

Get keys at:
- OpenRouter: https://openrouter.ai/keys
- Gemini: https://aistudio.google.com/apikey

## Development

```bash
# Copy environment template
cp .env.example .env

# Run in dev mode (hot reload)
./mvnw quarkus:dev

# Build
./mvnw clean package

# Run tests
./mvnw test
```

The API will be available at `http://localhost:8080`.

## Production Build

```bash
# Create native executable (requires GraalVM)
./mvnw package -Dnative

# Or create JVM jar
./mvnw clean package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

## License

MIT
