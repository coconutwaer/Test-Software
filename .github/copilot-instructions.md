# Copilot Instructions

## Repository Overview

This repository contains **Vinothek AI** — an AI-powered wine recommendation kiosk (`wine-ai-kiosk/`). Customers interact with an intelligent sommelier chatbot that recommends wines from the store's PostgreSQL inventory using the Langdock AI API (OpenAI-compatible).

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.2, Spring Data JPA
- **Database**: PostgreSQL (production), H2 (tests)
- **AI Integration**: Langdock API (OpenAI-compatible, model: `gpt-4o`)
- **Frontend**: Vanilla HTML/CSS/JavaScript (served as static files by Spring Boot)
- **Build tool**: Maven

## Project Structure

```
wine-ai-kiosk/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/winekiosk/
    │   │   ├── WineKioskApplication.java       # Spring Boot entry point
    │   │   ├── config/                         # CORS and Langdock config beans
    │   │   ├── model/                          # JPA entities (Wine, ChatLog)
    │   │   ├── repository/                     # Spring Data JPA repositories
    │   │   ├── service/                        # Business logic (WineService, ChatService, LangdockService)
    │   │   ├── controller/                     # REST controllers (WineController, ChatController, AdminController)
    │   │   └── dto/                            # Request/response DTOs
    │   └── resources/
    │       ├── application.properties          # App config (DB URL, Langdock API key)
    │       ├── schema.sql                      # DB schema
    │       ├── data.sql                        # Seed data
    │       └── static/                         # Frontend (index.html, admin.html, css/, js/)
    └── test/
        └── java/com/winekiosk/
            └── WineKioskApplicationTests.java
```

## Build & Run

All commands run from the `wine-ai-kiosk/` directory.

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Run tests
mvn test

# Run the application
java -jar target/wine-ai-kiosk-1.0.0.jar
```

The application starts on port **8080** by default.

## Configuration

Key properties in `src/main/resources/application.properties`:

| Property | Description |
|---|---|
| `spring.datasource.url` | PostgreSQL JDBC URL |
| `spring.datasource.username` | DB username |
| `spring.datasource.password` | DB password |
| `langdock.api.url` | Langdock API endpoint |
| `langdock.api.key` | Langdock API key (required) |
| `langdock.api.model` | AI model (default: `gpt-4o`) |

## Coding Conventions

- Use standard Spring Boot layered architecture: Controller → Service → Repository
- DTOs go in the `dto/` package; JPA entities go in `model/`
- Configuration beans belong in the `config/` package
- REST endpoints follow RESTful conventions; admin endpoints are under `/api/admin/`
- Tests use H2 in-memory database (configured automatically for `test` scope)
- Java code follows standard Java naming conventions (camelCase for methods/fields, PascalCase for classes)

## Key APIs

- `GET /api/wines` — list/filter wines
- `POST /api/chat` — send a message to the AI sommelier
- `GET/POST/PUT/DELETE /api/admin/wines` — admin CRUD for wine inventory
