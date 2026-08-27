# Vending Machine

This repository contains a take-home assignment for a full-stack vending machine application. The backend is being built with Spring Boot, while a separate React frontend will provide the user interface.

The backend and frontend live under dedicated top-level directories in this repository. A third project, `mock-api/`, represents the external source of the initial product catalog.

## Tech Stack

- Java 21
- Spring Boot 3
- Maven
- React and TypeScript for the separately developed frontend
- An external mocked API as the source of initial product data
- In-memory backend application state
- No database

## Repository Structure

```text
vending-machine/
├── backend/
│   ├── pom.xml
│   └── src/       # Spring Boot backend source
├── frontend/      # Reserved for the React and TypeScript application
├── mock-api/      # Read-only external mock products API
└── .github/       # GitHub Actions workflows
```

The `frontend/` directory currently contains no frontend implementation or generated project files.

## Architecture

The intended product-data flow is:

1. The external mocked API represents the source of the initial product catalog.
2. The Spring Boot application loads the initial products from that resource.
3. Products are then maintained in backend application memory.
4. CRUD operations modify only the application's in-memory state.
5. Those changes are intentionally not persisted back to the mocked external API.
6. Restarting the backend therefore resets the application's product state and reloads the initial catalog.
7. Vending operations and business rules will be handled by the Spring Boot backend.
8. React will act primarily as the presentation layer and communicate with the backend through REST APIs.

Keeping CRUD changes local to backend memory is an explicit architectural decision based on the assignment requirement that product changes do not need to update the external resource. The external API seeds the catalog; it is not the application's persistence layer.

## Planned Features

- Product CRUD operations
- A maximum stock of 15 items per product type
- Supported coin validation
- Coin insertion
- Product purchase
- Exact change calculation
- Transaction cancellation and refund
- A responsive React frontend

## Accepted Currency and Coins

The vending machine will use EUR and is planned to accept:

- €0.10
- €0.20
- €0.50
- €1.00
- €2.00

All monetary calculations will use integer cents rather than floating-point values.

## Running the Backend

Run the test suite and build checks:

```bash
cd backend
mvn clean test
```

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

The backend is configured to listen on port `8080`. Start the external mock API first; backend startup fails if the initial catalog cannot be loaded.

## Continuous Integration

GitHub Actions runs the Maven build and test suites for both Java projects with Java 21 on pushes to `main` and on pull requests. The workflow is defined in `.github/workflows/java-ci.yml`.

## Publishing to GitHub

After creating an empty repository on GitHub, create the first commit and connect this local repository:

```bash
git add .
git commit -m "Initial backend scaffold"
git remote add origin <repository-url>
git push -u origin main
```

Replace `<repository-url>` with the HTTPS or SSH URL shown by GitHub. Create the GitHub repository without generated README, `.gitignore`, or license files because those can conflict with the files already present locally.

## External Mock API

The independently runnable mock API serves the initial product catalog from a static JSON resource. It has no database, mutable state, or write endpoints.

Start it with:

```bash
cd mock-api
mvn spring-boot:run
```

It listens on port `3001`. Retrieve the catalog with:

```bash
curl http://localhost:3001/products
```

Each mock product includes its current `quantity`, with the initial values kept within the planned maximum stock of 15. Product prices are expressed in integer EUR cents. The backend's mock API base URL is configured with:

```properties
vending.external-api.base-url=http://localhost:3001
```

At startup, the Spring Boot backend calls `GET /products` at this base URL and replaces its in-memory product catalog with the returned products. The mutable application state is held in a map indexed by product ID. It is recreated from the external catalog on every backend restart and is never persisted back to the mock API.

## Product API

The backend exposes product operations under `/api/products`:

| Method | Endpoint | Behavior |
| --- | --- | --- |
| `GET` | `/api/products?page=0&size=20` | Lists active products in ascending ID order with pagination metadata |
| `GET` | `/api/products/{id}` | Returns one active product |
| `POST` | `/api/products` | Creates a product and returns `201 Created` |
| `PUT` | `/api/products/{id}` | Fully replaces an active product's editable fields |
| `DELETE` | `/api/products/{id}` | Soft-deletes a product and returns its last visible representation |

Create and update requests contain `name`, `price`, and `quantity`. Names must not be blank and may contain at most 100 characters, prices must be positive integer cents, and quantities must be between 0 and 15. IDs are assigned by the backend.

The list endpoint defaults to 20 items per page and accepts page sizes from 1 through 100. Its response contains `content`, `page`, `size`, `totalElements`, and `totalPages`.

Pagination is performed inside the in-memory repository. Products are indexed in ascending ID order, the active-product total is maintained as repository state, and each request allocates only the requested page rather than scanning and copying the entire catalog into the service layer. The controller depends on the `ProductService` interface; `ProductServiceImpl` contains the current application-service implementation.

Soft-deleted products remain in backend memory but are treated as absent by list, get, update, and repeated delete operations. The public product response deliberately does not expose the internal deletion marker. Invalid requests return `400 Bad Request`; missing or deleted products return `404 Not Found` using Spring's problem-details JSON format.

These endpoints operate only on backend memory. The external mock API is called during startup to seed the catalog and is never updated by product API requests.

## Project Status

The repository currently contains startup product loading, in-memory product CRUD APIs with pagination and soft deletion, the read-only external mock products API, and an empty `frontend/` directory. Vending-machine operations, transaction state, coin handling, and the React application will be implemented incrementally.
