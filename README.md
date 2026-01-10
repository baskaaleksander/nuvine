# Nuvine

Nuvine is a cloud-native, AI-powered knowledge management platform built with a microservices architecture. The platform
enables teams to organize, process, and interact with their documents using advanced RAG (Retrieval-Augmented
Generation) capabilities, semantic search, and conversational AI. Built on Spring Boot 3.5 and Java 21, Nuvine leverages
modern distributed systems patterns including event-driven architecture, service mesh, and distributed tracing to
deliver a scalable, resilient solution for enterprise document intelligence.

## Architecture Overview

Nuvine consists of 12 microservices orchestrated through Docker Compose, communicating via Apache Kafka for asynchronous
events and HTTP/REST for synchronous operations. The architecture follows Domain-Driven Design principles with clear
bounded contexts and a layered approach.

### System Architecture

```mermaid
graph TB
    Client[Client Applications]

    Client --> Gateway[API Gateway :8222]
    Gateway --> Discovery[Service Discovery :8761]

    Gateway --> Auth[Auth Service :8040]
    Gateway --> Workspace[Workspace Service :8060]
    Gateway --> Chat[Chat Service :8110]
    Gateway --> FileStorage[File Storage :8080]
    Gateway --> Subscription[Subscription :8120]

    Auth --> Keycloak[Keycloak OAuth2]

    Workspace --> Kafka[Apache Kafka]
    FileStorage --> MinIO[MinIO S3 Storage]
    FileStorage --> Kafka

    Kafka --> Ingestion[Ingestion Service :8080]
    Ingestion --> Vector[Vector Service :8100]
    Vector --> Qdrant[Qdrant Vector DB]

    Chat --> LLMRouter[LLM Router :8090]
    Chat --> Vector
    LLMRouter --> OpenAI[OpenAI API]
    LLMRouter --> OpenRouter[OpenRouter API]

    Kafka --> Notification[Notification :8050]
    Notification --> SMTP[SMTP Server]

    Auth --> Postgres[(PostgreSQL)]
    Workspace --> Postgres
    Ingestion --> Postgres
    Vector --> Postgres
    Subscription --> Postgres
    Chat --> Postgres
    LLMRouter --> Postgres

    Notification --> MongoDB[(MongoDB)]

    Auth --> Redis[(Redis)]
    Workspace --> Redis
    Vector --> Redis
    Chat --> Redis
    FileStorage --> Redis
    Gateway --> Redis

    subgraph Observability
        Zipkin[Zipkin :9411]
        Prometheus[Prometheus :9090]
        Grafana[Grafana :3000]
    end

    ConfigServer[Config Server :8888] -.-> Auth
    ConfigServer -.-> Workspace
    ConfigServer -.-> Chat
    ConfigServer -.-> FileStorage
    ConfigServer -.-> Ingestion
    ConfigServer -.-> Vector
    ConfigServer -.-> LLMRouter
    ConfigServer -.-> Notification
    ConfigServer -.-> Subscription

    Discovery --> ConfigServer

    style Gateway fill:#e1f5ff
    style Kafka fill:#fff4e1
    style Postgres fill:#e8f5e9
    style MongoDB fill:#e8f5e9
    style Redis fill:#ffe8e8
    style Keycloak fill:#f3e5f5
```

### Document Processing Pipeline

```mermaid
sequenceDiagram
    participant User
    participant Gateway
    participant FileStorage
    participant MinIO
    participant Kafka
    participant Ingestion
    participant Vector
    participant Qdrant
    participant Chat
    participant LLMRouter

    User->>Gateway: Upload document
    Gateway->>FileStorage: POST /api/v1/files
    FileStorage->>MinIO: Store document
    MinIO-->>FileStorage: Storage key
    FileStorage->>Kafka: Publish DocumentUploaded event
    FileStorage-->>User: Upload confirmed

    Kafka->>Ingestion: Consume DocumentUploaded
    Ingestion->>MinIO: Fetch document bytes
    Ingestion->>Ingestion: Extract text (PDF/DOCX/etc)
    Ingestion->>Ingestion: Chunk text (700 tokens, 120 overlap)
    Ingestion->>Kafka: Publish chunks for vectorization

    Kafka->>Vector: Consume embedding request
    Vector->>LLMRouter: POST /api/v1/internal/llm/embeddings
    LLMRouter->>OpenAI: Create embeddings
    OpenAI-->>LLMRouter: Embedding vectors
    LLMRouter-->>Vector: Embedding response
    Vector->>Qdrant: Store vectors with metadata
    Vector->>Kafka: Publish VectorProcessingCompleted

    Note over Qdrant: Document now searchable

    User->>Gateway: Ask question about document
    Gateway->>Chat: POST /api/v1/chat/completions/stream
    Chat->>Vector: Semantic search for relevant chunks
    Vector->>Qdrant: Query similar vectors
    Qdrant-->>Vector: Top-k results
    Vector-->>Chat: Relevant document chunks
    Chat->>LLMRouter: POST /api/v1/internal/llm/completions
    LLMRouter->>OpenRouter: Stream completion with context
    OpenRouter-->>LLMRouter: Stream response tokens
    LLMRouter-->>Chat: Stream chunks
    Chat-->>User: Real-time answer with sources
```

## Tech Stack

- **Platform**: Java 21, Spring Boot 3.5 (Spring Cloud)
- **Messaging**: Apache Kafka
- **Datastores**: PostgreSQL (primary), Redis (cache/rate limiting), MongoDB (notifications)
- **Auth**: Keycloak (OAuth2/OIDC)
- **Storage**: MinIO (S3)
- **AI/RAG**: Qdrant (vector DB), OpenAI + OpenRouter (LLM/embeddings)
- **Observability**: Prometheus, Grafana, Zipkin, Micrometer

## Quick Start

### Prerequisites

Ensure you have the following installed:

- Docker 25.0+ and Docker Compose
- Java 21 JDK (for local development)
- Maven 3.9+ (for building services)

### Environment Variables

The platform requires a `.env` file in `infra/docker`. Copy `.env.example` and adjust the values:

```bash
cp .env.example infra/docker/.env
```

Key groups included in `.env.example`:

- **Service databases (PostgreSQL)**: `AUTH_DB_*`, `WORKSPACE_DB_*`, `CHAT_DB_*`, `SUBSCRIPTION_DB_*`, `INGESTION_DB_*`, `LLM_ROUTER_DB_*`, `VECTOR_DB_*`
- **Notification database (MongoDB)**: `NOTIFICATION_MONGO_*`
- **Auth/identity (Keycloak)**: `KC_*`, `KEYCLOAK_*`, plus per-service client secrets `KEYCLOAK_*_SERVICE_SECRET`
- **Messaging**: `KAFKA_HOST`
- **Vector DB**: `QDRANT_HOST`
- **Object storage (MinIO)**: `MINIO_*`
- **Email (notifications)**: `NOTIF_MAIL_*`
- **LLM providers**: `OPENAI_API_KEY`, `OPENROUTER_API_KEY`
- **Frontend/Stripe/Redis**: `FRONTEND_URL`, `STRIPE_*`, `REDIS_PASSWORD`

Keycloak realm/client setup: see `infra/keycloak/KEYCLOAK_SETUP.md` (realm import + client secrets).

### Running with Docker Compose

1. Create env file:
   ```bash
   cp .env.example infra/docker/.env
   ```

2. Configure Keycloak realm export + client secrets:
   See `infra/keycloak/KEYCLOAK_SETUP.md`.

   If you need to re-import the realm, delete the Keycloak Docker volume and restart (details in the guide).

3. Start the stack:
   ```bash
   cd infra/docker
   docker compose up -d
   ```

Access the platform:
- API Gateway: http://localhost:8222
- Keycloak Admin: http://localhost:9098 (admin credentials in `.env`)
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Zipkin: http://localhost:9411
- MinIO Console: http://localhost:9001

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
cd services/auth
mvn test

# Run with coverage
mvn test jacoco:report
# Report: target/site/jacoco/index.html

# Run integration tests only
mvn test -Dgroups=integration
```

## Service Details

### Config Server (Port 8888)

Centralized configuration server using Spring Cloud Config. All microservices fetch their configuration from this server
on startup. Must be started first before any other service.

**Technology**: Spring Cloud Config Server, Git backend
**Dependencies**: None (must start first)

### Discovery Service (Port 8761)

Netflix Eureka server providing service registration and discovery. Services register themselves on startup and discover
other services dynamically.

**Technology**: Spring Cloud Netflix Eureka
**Dependencies**: Config Server

### API Gateway (Port 8222)

Spring Cloud Gateway providing unified entry point, request routing, authentication, rate limiting, and CORS handling.
Routes requests to appropriate microservices based on path patterns.

**Technology**: Spring Cloud Gateway, Redis (rate limiting)
**Key Routes**: /api/v1/auth/*, /api/v1/workspaces/*, /api/v1/files/*, /api/v1/chat/*, /api/v1/subscription/*

### Auth Service (Port 8040)

Handles user registration, authentication, email verification, password reset, and OAuth2 integration with Keycloak.
Issues JWT tokens for service-to-service and client authentication.

**Technology**: Spring Security, Keycloak, PostgreSQL, Redis, Kafka
**Key Endpoints**: /api/v1/auth/register, /api/v1/auth/login, /api/v1/auth/verify-email, /api/v1/auth/reset-password

### Workspace Service (Port 8060)

Multi-tenant workspace management with projects, documents, and team collaboration. Handles workspace hierarchy,
role-based access control (OWNER/ADMIN/MEMBER), and invitation system.

**Technology**: Spring Boot, PostgreSQL, Redis, Kafka
**Key Endpoints**: /api/v1/workspaces/*, /api/v1/projects/*, /api/v1/documents/*, /api/v1/members/*

### File Storage Service (Port 8080)

S3-compatible file storage using MinIO. Generates presigned URLs for secure uploads/downloads, manages file metadata,
and publishes document upload events to Kafka.

**Technology**: Spring Boot, AWS SDK for Java, MinIO, Redis, Kafka
**Key Endpoints**: /api/v1/files/upload, /api/v1/files/{id}, /api/v1/files/{id}/presigned-url

### Ingestion Service (Port 8080)

Document processing pipeline orchestrator. Extracts text from multiple formats (PDF, DOCX, PPTX, HTML, Markdown), chunks
text with semantic overlap (700 tokens, 120 overlap), and coordinates vectorization.

**Technology**: Spring Boot, Apache PDFBox, Apache POI, PostgreSQL, Kafka
**Supported Formats**: PDF, DOCX, PPTX, HTML, Markdown, Plain Text
**Key Endpoints**: /api/v1/internal/ingestion/jobs, /api/v1/internal/ingestion/jobs/{id}/start

### Vector Service (Port 8100)

Manages vector embeddings and semantic search using Qdrant. Stores chunk-to-vector mappings, performs similarity
searches, and provides retrieval for RAG pipelines.

**Technology**: Spring Boot, Qdrant (gRPC), PostgreSQL, Redis, Kafka
**Key Endpoints**: /api/v1/internal/vector/search, /api/v1/internal/vector/search-by-text,
/api/v1/internal/vector/collections/{id}

### LLM Router Service (Port 8090)

Unified gateway for LLM operations. Routes requests to OpenAI and OpenRouter with circuit breaker protection, handles
streaming responses, and manages text embeddings.

**Technology**: Spring Boot, OpenAI Java Client, WebClient, Resilience4j, PostgreSQL, Kafka
**Key Endpoints**: /api/v1/internal/llm/completions, /api/v1/internal/llm/completion/stream,
/api/v1/internal/llm/embeddings

### Chat Service (Port 8110)

RAG-powered conversational AI with streaming support. Orchestrates semantic search, context retrieval, and LLM
completion generation. Maintains conversation history and provides real-time streaming responses.

**Technology**: Spring Boot, PostgreSQL, Redis, Kafka, WebFlux
**Key Endpoints**: /api/v1/chat/completions, /api/v1/chat/completions/stream, /api/v1/chat/conversations/*

### Notification Service (Port 8050)

Event-driven email delivery system. Consumes domain events from Kafka, renders HTML emails using Thymeleaf templates,
and persists encrypted notification records in MongoDB.

**Technology**: Spring Boot, MongoDB, Kafka, JavaMailSender, Thymeleaf
**Notification Types**: User registration, email verification, password reset, workspace invitations, payment
notifications

### Subscription Service (Port 8120)

Manages billing, token quota tracking, and Stripe payment processing. Handles subscription lifecycle, usage monitoring,
webhook processing, and customer portal access.

**Technology**: Spring Boot, Stripe Java SDK, PostgreSQL, Redis, Kafka
**Key Endpoints**: /api/v1/billing/*, /api/v1/subscription/*, /api/v1/usage/*

## Database Migrations

Services use Flyway for database migrations. Migrations run automatically on startup.

```bash
# Manually run migrations
cd services/auth
mvn flyway:migrate

# Rollback last migration
mvn flyway:undo

# Check migration status
mvn flyway:info
```

## Contributing

Development follows standard Git workflow:

1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes and test locally
3. Run tests: `mvn test`
4. Commit with conventional commits: `git commit -m "feat: add new feature"`
5. Push and create pull request

## License

Copyright 2025 Aleksander Baska. All rights reserved.
