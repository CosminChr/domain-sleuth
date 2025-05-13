# Domain Sleuth - OSINT Domain Scanner

Domain Sleuth is a web application that leverages Docker and TheHarvester to perform OSINT (Open Source Intelligence) scans on domain names. It helps security researchers and professionals gather publicly available information about domains such as email addresses, hostnames, and IP addresses.

![Domain Sleuth Screenshot](assets/Screenshot%202025-05-13%20at%2022.36.06.png)

## Features

- Simple, user-friendly web interface
- Docker-based scanning with TheHarvester
- Real-time scan status updates (only for active scans)
- Persistent history of previous scans
- Detailed results view with parsed findings
- Asynchronous scan processing
- Smart polling that only updates active scans (PENDING or IN_PROGRESS)

## Architecture

The application consists of two main components:

- **Backend**: Spring Boot application written in Kotlin that manages scans and interacts with Docker
- **Frontend**: React application written in TypeScript that provides the user interface

## Prerequisites

- Docker and Docker Compose
- Java 21 (for development)
- Node.js (for frontend development)

## Quick Start with Docker Compose

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/domain-sleuth.git
   cd domain-sleuth
   ```

2. Start the application using the provided build script:
   ```bash
   ./build-and-run.sh
   ```

   Or manually with Docker Compose:
   ```bash
   docker-compose up -d
   ```

3. Access the application in your browser:
   ```
   http://localhost:5173
   ```

## Utility Scripts

The project includes several utility scripts to help with development and deployment:

### build-and-run.sh
Builds both the backend and frontend applications and starts all services using Docker Compose.

```bash
./build-and-run.sh
```

### infra-cleanup.sh
Cleans up all Docker resources related to the project (containers, volumes, networks).

```bash
./infra-cleanup.sh
```

This is useful when you want to completely reset your development environment or free up resources.

## Development Setup

### Backend

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Build the application:
   ```bash
   ./gradlew build
   ```

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

### Frontend

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```

## API Documentation

The backend exposes the following RESTful API endpoints:

- `POST /api/scans` - Initiates a new scan for a domain
- `GET /api/scans` - Retrieves all scan history
- `GET /api/scans/{id}` - Retrieves a specific scan by ID

## Docker Configuration

The application uses Docker in two ways:

1. As a runtime environment for the application itself (via Docker Compose)
2. As a tool for running Amass OSINT scans in isolated containers

The backend application needs access to the Docker socket to create and manage scan containers.

## License

[MIT License](LICENSE)