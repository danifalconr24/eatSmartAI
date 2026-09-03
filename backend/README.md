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
│   ├── AnalyzeProductUseCase.java
│   ├── GenerateShoppingListUseCase.java
│   ├── ChatWithNutritionistUseCase.java
│   └── ... (prompt builders + result parsers)
├── domain/            # Domain models and exceptions
│   ├── model/
│   └── exception/
└── infrastructure/    # External integrations
    ├── rest/          # REST API endpoints
    ├── openrouter/    # OpenRouter AI client (primary)
    └── gemini/        # Gemini AI client (fallback)
```

## Error contract

Each endpoint returns `ErrorResponse {"message": "..."}` with Spanish, user-facing text:

- **400** — valid business rejection: unreadable receipt, unrecognizable product, invalid request.
- **502** — technical failure after trying all enabled providers. The message is intentionally **generic** ("No se pudo completar el análisis. Inténtalo de nuevo en unos minutos."): provider names/details (OpenRouter, Gemini, HTTP statuses) are logged server-side only and never exposed to clients. The original exception is chained as the cause for logs.

The app relies on this contract: `400` consumes one user credit (the analysis ran), `502`/network failures do not.

## API Endpoint

### `POST /api/chat`

Answers user questions about a previous analysis (receipt or product), nutritionist-style. Stateless: the client resends the analysis context and the full message history on every request.

**Request (application/json):**
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| question | string | Yes | User question |
| products | string[] | Receipt chats | Products detected in the receipt |
| suggestions | string | Receipt chats | Suggestions markdown from the analysis |
| product | string | Product chats | Detected product name |
| nutrition | string | Product chats | Nutrition markdown from the analysis |
| score | int | No | Analysis score (0-10) |
| goal | string | Yes | `LOSE`, `MAINTAIN`, or `GAIN` |
| dietPreference | string | No | `NONE`, `VEGETARIAN`, `VEGAN`, or `OTHER` |
| budgetMatters | boolean | No | Consider budget in answers |
| allergies | string | No | Comma-separated allergies |
| messages | array | No | Previous turns `[{role: "user"\|"assistant", content}]` (max 20 kept) |

**Response (200):**
```json
{
  "answer": "Mejor pan integral: más fibra y saciedad..."
}
```

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
