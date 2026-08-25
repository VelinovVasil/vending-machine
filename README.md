# Vending Machine

This repository contains a take-home assignment for a full-stack vending machine application. The backend is being built with Spring Boot, while a separate React frontend will provide the user interface.

Both applications live under dedicated top-level directories in this repository. The `backend/` directory contains the Spring Boot application, and the `frontend/` directory is reserved for the future React application.

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

- Initial product loading from the external mocked API
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

The backend is configured to listen on port `8080`.

## Continuous Integration

GitHub Actions runs the Maven build and test suite with Java 21 for pushes to `main` and for pull requests. The workflow is defined in `.github/workflows/backend-ci.yml`.

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

A mocked products API will be added separately. Its base URL is configured with:

```properties
vending.external-api.base-url=http://localhost:3001
```

No mocked API implementation is included in this initial setup.

## Project Status

The repository currently contains the initial backend scaffold under `backend/` plus an empty `frontend/` directory for the future React application. Vending-machine functionality, REST APIs, product integration, in-memory state management, and the React application will be implemented incrementally.
