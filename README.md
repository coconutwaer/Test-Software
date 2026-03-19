# Test-Software

## 🍷 Vinothek AI — Wine Recommendation Kiosk

This repository contains the **Vinothek AI** webapp: an AI-powered wine recommendation kiosk built with Spring Boot, PostgreSQL, and the Langdock AI API.

---

## Prerequisites

- **Java 21** (Eclipse Temurin recommended — <https://adoptium.net/temurin/releases/?version=21>)
- **Apache Maven 3.9+** (<https://maven.apache.org/download.cgi>)
- **PostgreSQL 15+** (<https://www.postgresql.org/download/>)
- A **Langdock API key** (<https://langdock.com>)

---

## Quick Start

### 1. Create the database

Open **pgAdmin** or **psql** and run:

```sql
CREATE DATABASE winekiosk;
```

### 2. Configure the application

Edit `wine-ai-kiosk/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/winekiosk
spring.datasource.username=postgres
spring.datasource.password=YOUR_POSTGRES_PASSWORD

langdock.api.key=YOUR_LANGDOCK_API_KEY_HERE
```

### 3. Build the application

From the `wine-ai-kiosk/` directory:

```bash
cd wine-ai-kiosk
mvn clean package -DskipTests
```

This produces `target/wine-ai-kiosk-1.0.0.jar`.

### 4. Run the application

```bash
java -jar target/wine-ai-kiosk-1.0.0.jar
```

### 5. Open the webapp

| Interface       | URL                               |
|-----------------|-----------------------------------|
| Kiosk / Chat    | <http://localhost:8080>           |
| Admin panel     | <http://localhost:8080/admin.html> |

---

For full setup details (Windows instructions, API docs, configuration reference, project structure) see [`wine-ai-kiosk/README.md`](wine-ai-kiosk/README.md).
