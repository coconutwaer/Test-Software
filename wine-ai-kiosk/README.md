# 🍷 Vinothek AI — Wine Recommendation Kiosk

An AI-powered wine recommendation kiosk for wine stores. Customers interact with an intelligent sommelier chatbot that recommends wines from the store's PostgreSQL inventory using the Langdock AI API (OpenAI-compatible).

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser (Kiosk)                       │
│  ┌──────────────────┐          ┌──────────────────────────┐  │
│  │  Sommelier Chat  │          │    Browse / Filter UI    │  │
│  │  (index.html)    │          │    (index.html)          │  │
│  └────────┬─────────┘          └──────────┬───────────────┘  │
│           │  POST /api/chat                │  GET/POST /api/wines │
└───────────┼────────────────────────────────┼───────────────────┘
            │                                │
            ▼                                ▼
┌───────────────────────────────────────────────────────────────┐
│              Spring Boot 3.2 Backend (port 8080)              │
│                                                               │
│  ChatController   WineController   AdminController            │
│       │                 │                 │                   │
│  ChatService       WineService       WineService              │
│       │                 │                                     │
│  LangdockService   WineRepository                             │
│       │                 │                                     │
└───────┼─────────────────┼─────────────────────────────────────┘
        │                 │
        ▼                 ▼
┌──────────────┐   ┌──────────────────┐
│  Langdock AI │   │   PostgreSQL DB  │
│  (OpenAI API)│   │  (wines table)   │
└──────────────┘   └──────────────────┘
```

---

## Tech Stack

| Layer      | Technology                          |
|------------|-------------------------------------|
| Backend    | Java 21, Spring Boot 3.2.x          |
| ORM        | Spring Data JPA, Hibernate          |
| Database   | PostgreSQL 15+                      |
| AI API     | Langdock (OpenAI-compatible)        |
| Frontend   | HTML5, TailwindCSS (CDN), Vanilla JS |
| Build      | Apache Maven 3.9+                   |

---

## Prerequisites

- Java 21 (Eclipse Temurin recommended)
- Apache Maven 3.9+
- PostgreSQL 15+
- A Langdock API key (<https://langdock.com>)

---

## Windows Setup Instructions

### 1. Install Java 21

1. Download Eclipse Temurin 21 from <https://adoptium.net/temurin/releases/?version=21>
2. Run the installer. Make sure "Add to PATH" and "Set JAVA_HOME" are checked.
3. Verify: open Command Prompt and run `java -version` — it should show `21.x.x`

### 2. Install Maven

1. Download Maven binary ZIP from <https://maven.apache.org/download.cgi>
2. Extract to e.g. `C:\maven`
3. Add `C:\maven\bin` to your system PATH
4. Verify: `mvn -version`

### 3. Install PostgreSQL

1. Download PostgreSQL installer from <https://www.postgresql.org/download/windows/>
2. Run installer. Note the password you set for the `postgres` user.
3. Keep the default port: **5432**

### 4. Create the Database

Open **pgAdmin** or **psql** and run:

```sql
CREATE DATABASE winekiosk;
```

### 5. Configure the Application

Open `src/main/resources/application.properties` and update:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/winekiosk
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD

langdock.api.url=https://api.langdock.com/v1/chat/completions
langdock.api.key=YOUR_LANGDOCK_API_KEY_HERE
langdock.api.model=gpt-4o
```

### 6. Build the Application

In the `wine-ai-kiosk/` directory:

```cmd
mvn clean package -DskipTests
```

This produces `target/wine-ai-kiosk-1.0.0.jar`.

### 7. Run the Application

```cmd
java -jar target/wine-ai-kiosk-1.0.0.jar
```

### 8. Open the Kiosk

Navigate to: <http://localhost:8080>

Admin interface: <http://localhost:8080/admin.html>

---

## API Documentation

### Chat

| Method | Endpoint    | Description                            |
|--------|-------------|----------------------------------------|
| POST   | /api/chat   | Send message, get AI recommendation    |

**Request:**
```json
{ "message": "I want a fruity red wine for pasta under 15€" }
```

**Response:**
```json
{
  "reply": "I recommend the **Primitivo di Manduria**...",
  "recommendedWines": [ { "id": 4, "name": "...", ... } ]
}
```

### Wines (Customer)

| Method | Endpoint              | Description                  |
|--------|-----------------------|------------------------------|
| GET    | /api/wines            | Get all wines                |
| GET    | /api/wines/{id}       | Get wine by ID               |
| POST   | /api/wines/filter     | Filter wines by criteria     |
| GET    | /api/wines/filters    | Get available filter options |

**Filter Request Body:**
```json
{
  "wineType": "Red",
  "country": "Italy",
  "region": "Toscana",
  "grapeVariety": "Sangiovese",
  "sweetness": "Dry",
  "body": "Medium",
  "minPrice": 5.00,
  "maxPrice": 25.00,
  "minRating": 4.0,
  "searchText": "cherry"
}
```

### Admin

| Method | Endpoint              | Description       |
|--------|-----------------------|-------------------|
| GET    | /api/admin/wines      | List all wines    |
| POST   | /api/admin/wines      | Create wine       |
| PUT    | /api/admin/wines/{id} | Update wine       |
| DELETE | /api/admin/wines/{id} | Delete wine       |

---

## Configuration Reference

| Property                      | Description                        | Default                                          |
|-------------------------------|------------------------------------|--------------------------------------------------|
| `server.port`                 | HTTP port                          | 8080                                             |
| `spring.datasource.url`       | PostgreSQL JDBC URL                | jdbc:postgresql://localhost:5432/winekiosk       |
| `spring.datasource.username`  | DB username                        | postgres                                         |
| `spring.datasource.password`  | DB password                        | postgres                                         |
| `langdock.api.url`            | Langdock API endpoint              | https://api.langdock.com/v1/chat/completions     |
| `langdock.api.key`            | Your Langdock API key              | (required)                                       |
| `langdock.api.model`          | Model to use                       | gpt-4o                                           |

---

## AI Sommelier Behavior

The chatbot (Vinothek AI) follows a two-call AI pattern:

1. **Parameter Extraction**: User message → Langdock API → structured JSON search params
2. **Recommendation**: Matched wines from DB → Langdock API with sommelier system prompt → natural language reply

The AI is instructed to:
- **Only** recommend wines present in the current database inventory
- Explain why each wine matches the customer's request
- Describe flavor profiles, food pairings, and value for money
- Keep responses concise and conversational

---

## Database Seed Data

The application ships with 20 realistic wines covering:

- **Countries**: Germany, Italy, France, Austria, New Zealand, Chile, USA, Australia, South Africa
- **Regions**: Franken, Pfalz, Baden, Rheingau, Nahe, Toscana, Puglia, Rhone, Provence, Bourgogne, Wachau, Sudtirol, Maipo Valley, California, Barossa Valley, Marlborough, Stellenbosch
- **Grapes**: Riesling, Grauburgunder, Weißburgunder, Sangiovese, Primitivo, Shiraz, Chardonnay, Grüner Veltliner, Cabernet Sauvignon, Merlot, Lagrein, Sauvignon Blanc, Nero d'Avola, Chenin Blanc, Cinsault

Seeds are idempotent — they only insert if the wines table is empty.

---

## Screenshots

- **Sommelier Tab**: Chat interface with dark wine-themed UI, AI conversation, and recommended wine cards
- **Browse Tab**: Wine grid with filters for type, country, region, grape, sweetness, and price
- **Wine Detail Modal**: Full wine information including flavor notes, food pairings, and description
- **Admin Panel**: Table of all wines with add/edit/delete functionality

---

## Project Structure

```
wine-ai-kiosk/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/winekiosk/
    │   │   ├── WineKioskApplication.java
    │   │   ├── config/
    │   │   │   ├── CorsConfig.java
    │   │   │   └── LangdockConfig.java
    │   │   ├── model/
    │   │   │   ├── Wine.java
    │   │   │   └── ChatLog.java
    │   │   ├── repository/
    │   │   │   ├── WineRepository.java
    │   │   │   └── ChatLogRepository.java
    │   │   ├── service/
    │   │   │   ├── WineService.java
    │   │   │   ├── ChatService.java
    │   │   │   └── LangdockService.java
    │   │   ├── controller/
    │   │   │   ├── ChatController.java
    │   │   │   ├── WineController.java
    │   │   │   └── AdminController.java
    │   │   └── dto/
    │   │       ├── ChatRequest.java
    │   │       ├── ChatResponse.java
    │   │       └── WineFilterRequest.java
    │   └── resources/
    │       ├── application.properties
    │       ├── schema.sql
    │       ├── data.sql
    │       └── static/
    │           ├── index.html
    │           ├── admin.html
    │           ├── css/
    │           │   └── style.css
    │           └── js/
    │               ├── app.js
    │               └── admin.js
    └── test/
        └── java/com/winekiosk/
            └── WineKioskApplicationTests.java
```
