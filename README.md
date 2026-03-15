# Smart Order Manager - Hospitality Order Orchestrator

Order Orchestrator Application for Hospitality built with **Spring Boot**, **WebFlux**, and **Apache Camel Saga** pattern.

## Architecture

This application follows the **Microservice Orchestrator Pattern** with contract-first API design aligned to the **Traveler MMF (Minimum Marketable Feature) Specification** for hospitality.

### Key Components

- **WebFlux Reactive APIs**: Non-blocking, reactive REST endpoints for order CRUD operations
- **Apache Camel Saga**: Distributed transaction orchestration with compensation/rollback support
- **Contract-First**: OpenAPI 3.0 specification defined upfront at `src/main/resources/openapi/order-api.yaml`
- **Traveler MMF**: Domain models supporting traveler profiles, dietary preferences, hospitality order types

### Saga Orchestration Flow

```
Create Order Saga:
  1. Reserve Inventory    ←→ Release Inventory (compensation)
  2. Process Payment      ←→ Refund Payment (compensation)
  3. Notify Service       ←→ Cancel Notification (compensation)

Modify Order Saga:
  1. Adjust Inventory     ←→ Revert Adjustment (compensation)
  2. Recalculate Payment  ←→ Revert Recalculation (compensation)
  3. Update Service       ←→ Revert Update (compensation)

Cancel Order Saga:
  1. Cancel Service       ←→ Reinstate Service (compensation)
  2. Process Refund       ←→ Reverse Refund (compensation)
  3. Release Inventory    ←→ Re-reserve Inventory (compensation)
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create a new order |
| GET | `/api/v1/orders` | List all orders (with optional filters) |
| GET | `/api/v1/orders/{orderId}` | Get order by ID |
| PUT | `/api/v1/orders/{orderId}` | Modify an order |
| POST | `/api/v1/orders/{orderId}/cancel` | Cancel an order |
| GET | `/api/v1/health` | Health check |

## Order Types (Hospitality)

- `ROOM_SERVICE` - In-room food & beverage
- `RESTAURANT_DINING` - Restaurant reservations & orders
- `BAR_LOUNGE` - Bar and lounge orders
- `SPA_WELLNESS` - Spa and wellness services
- `AMENITY_REQUEST` - Room amenity requests
- `LAUNDRY` - Laundry services
- `CONCIERGE` - Concierge services

## Prerequisites

- Java 17+
- Maven 3.8+

## Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Run tests
mvn test
```

## API Documentation

Once running, access:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI Spec: http://localhost:8080/api-docs
- OpenAPI Contract: `src/main/resources/openapi/order-api.yaml`

## Tech Stack

- Spring Boot 3.2.5
- Spring WebFlux (Reactive)
- Apache Camel 4.4.0 (Saga EIP)
- SpringDoc OpenAPI
- Lombok
- JUnit 5 + Reactor Test
