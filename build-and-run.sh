#!/bin/bash

echo "====== Building and Running Domain Sleuth Application ======"

echo -e "\n${BLUE}=== Building the application ===${NC}"
echo -e "${YELLOW}Building the backend with Gradle...${NC}"
./gradlew clean build
if [ $? -eq 0 ]; then
    echo -e "${GREEN}Backend build completed successfully.${NC}"
else
    echo -e "${RED}Backend build failed. Continuing with cleanup anyway.${NC}"
fi

echo -e "${YELLOW}Building the frontend with npm...${NC}"
if [ -d "frontend" ]; then
    cd frontend
    npm run build
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}Frontend build completed successfully.${NC}"
    else
        echo -e "${RED}Frontend build failed. Continuing with cleanup anyway.${NC}"
    fi
    cd ..
else
    echo -e "${RED}Frontend directory not found. Skipping frontend build.${NC}"
fi

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
  echo "Docker is not running. Please start Docker first."
  exit 1
fi

echo "Building and starting all containers with Docker Compose..."
# Build all Docker images
docker-compose build
if [ $? -ne 0 ]; then
    echo "Docker Compose build failed!"
    exit 1
fi

# Start all services (postgres, backend, and frontend)
docker-compose up -d
if [ $? -ne 0 ]; then
    echo "Docker Compose up failed!"
    exit 1
fi

echo "====== Application started successfully! ======"
echo "Frontend available at: http://localhost:5173"
echo "Backend API available at: http://localhost:5173/api/scans"

# Check if services are running
echo -e "\nContainer status:"
docker-compose ps
