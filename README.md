# Scheduling System

A full-stack scheduling system that allows users to create and manage scheduled executions of predefined tasks. Built with React, Java Spring Boot, Quartz Scheduler, and PostgreSQL.

## Quick Start

### Prerequisites

- Docker and Docker Compose installed

### Run the Entire System

```bash
docker-compose up --build
```

Once started:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api

### Run Locally (Development)

#### Backend

Requires Java 17+ and Maven 3.9+.

```bash
# Start a PostgreSQL instance (or use the Docker one)
docker-compose up db

# Run the backend
cd backend
mvn spring-boot:run
```

#### Frontend

Requires Node.js 18+.

```bash
cd frontend
npm install
npm run dev
```

The dev server starts at http://localhost:3000 and proxies `/api` requests to the backend at port 8080.

### Run Tests

```bash
# Backend unit + integration tests (needs Docker for Testcontainers)
cd backend
mvn test

# Frontend tests
cd frontend
npm test
```

## Project Structure

```
scheduling-system/
  frontend/             # React + Vite + MUI + TypeScript
    src/
      api/              # Axios API clients
      components/       # React components (ScheduleTable, ScheduleDialog, etc.)
      types/            # TypeScript type definitions
      test/             # Test setup
  backend/              # Java 17 + Spring Boot 3 + Quartz
    src/main/java/com/scheduling/
      config/           # Quartz configuration
      controller/       # REST controllers + global exception handler
      dto/              # Request/Response DTOs
      job/              # Quartz job executor
      model/            # JPA entities and enums
      repository/       # Spring Data JPA repositories
      service/          # Business logic (ScheduleService, QuartzService)
      task/             # Predefined tasks and TaskRegistry
    src/test/           # Unit and integration tests
  docker/               # Dockerfiles
  docker-compose.yml    # Full-stack orchestration
```

## Design Decisions

### Task Registry Pattern

Predefined tasks are registered as Spring beans implementing a `ScheduledTask` interface. A `TaskRegistry` component auto-discovers all task beans at startup. This makes it trivial to add new tasks -- just implement the interface and annotate with `@Component`.

### Quartz Scheduler with JDBC JobStore

Quartz is configured with a JDBC-backed JobStore (PostgreSQL). This ensures scheduled jobs survive application restarts. The Quartz tables are auto-initialized via Spring Boot's `spring.quartz.jdbc.initialize-schema=always`.

### Schedule Type Mapping

Each schedule type maps to a specific Quartz trigger:
- **ONE_TIME** -> `SimpleTrigger` (fires once)
- **RECURRING** -> `SimpleTrigger` with repeat interval
- **WEEKLY** -> `CronTrigger` built from selected days + time
- **CRON** -> `CronTrigger` from raw cron expression

### Parameter Schema

Each task defines a parameter schema (`name`, `type`, `required`, `description`). The frontend dynamically renders input fields based on this schema. The backend validates required parameters before scheduling.

### Frontend (MUI + DataGrid)

Material UI was chosen for its rich component library. The DataGrid provides sorting, pagination, and a clean table layout. Schedule creation/editing is done via a Dialog with conditional form fields based on the selected schedule type.

### Database

PostgreSQL 16 was chosen for robust Quartz JDBC support and general reliability. Application tables are managed by Hibernate auto-DDL.

## Predefined Tasks

| Task | Description | Parameters |
|------|-------------|------------|
| Log Task | Writes a message to the application log | `message` (string, required) |
| HTTP Ping Task | Pings a URL and logs the HTTP status code | `url` (string, required), `timeout` (number, optional) |
| Database Query Task | Runs a read-only SQL query and logs row count | `query` (string, required) |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/tasks` | List all predefined tasks with parameter schemas |
| GET | `/api/schedules` | List all schedules |
| GET | `/api/schedules/{id}` | Get a single schedule |
| POST | `/api/schedules` | Create a new schedule |
| PUT | `/api/schedules/{id}` | Update an existing schedule |
| DELETE | `/api/schedules/{id}` | Delete a schedule |

## Assumptions

- No authentication/authorization is implemented. This is a demonstration system.
- Predefined tasks are hardcoded and not configurable via the UI (as specified).
- The system runs in a single-instance mode (Quartz clustering is disabled).
- All times are in the server's local timezone.
- The `DbQueryTask` only executes SELECT queries for safety (though this is enforced by convention, not technically restricted).

## AI Tools Used

- **Cursor (with Claude)**: Used for code generation, project scaffolding, and test writing. All generated code was reviewed and adjusted for correctness, consistency, and best practices.
