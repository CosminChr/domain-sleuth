#!/bin/bash

# Docker Project-Specific Cleanup Script
# This script removes containers, volumes, and networks related to the Domain Sleuth project

# Set text colors for better output readability
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Domain Sleuth Project Cleanup ===${NC}"
echo -e "${BLUE}=== Stopping and and removing all running containers, networks, and volumes ===${NC}"
echo -e "${YELLOW}Current Docker resources:${NC}"

# Display current Docker disk usage
docker system df

echo -e "\n${BLUE}=== Step 1: Stopping all containers using docker-compose ===${NC}"
if [ -f "docker-compose.yml" ]; then
    echo -e "${YELLOW}Found docker-compose.yml, stopping services...${NC}"
    docker-compose down || true
    echo -e "${GREEN}Docker Compose services stopped (if any).${NC}"
else
    echo -e "${YELLOW}No docker-compose.yml found in current directory.${NC}"
fi

echo -e "\n${BLUE}=== Step 2: Stopping all remaining containers ===${NC}"
RUNNING_CONTAINERS=$(docker ps -q)
if [ -n "$RUNNING_CONTAINERS" ]; then
    echo -e "${YELLOW}Stopping all running containers...${NC}"
    docker stop $RUNNING_CONTAINERS
    echo -e "${GREEN}All running containers stopped.${NC}"
else
    echo -e "${GREEN}No running containers found.${NC}"
fi

echo -e "\n${BLUE}=== Step 3: Removing all containers ===${NC}"
ALL_CONTAINERS=$(docker ps -a -q)
if [ -n "$ALL_CONTAINERS" ]; then
    echo -e "${YELLOW}Removing all containers...${NC}"
    docker rm -f $ALL_CONTAINERS
    echo -e "${GREEN}All containers removed.${NC}"
else
    echo -e "${GREEN}No containers to remove.${NC}"
fi

echo -e "\n${BLUE}=== Step 4: Removing project networks ===${NC}"
NETWORKS=$(docker network ls | grep -E "osint-network|domain-sleuth" | awk '{print $1}')
if [ -n "$NETWORKS" ]; then
    echo -e "${YELLOW}Removing project networks...${NC}"
    for network in $NETWORKS; do
        docker network rm $network || true
    done
    echo -e "${GREEN}Project networks removed (if not in use).${NC}"
else
    echo -e "${GREEN}No project networks found.${NC}"
fi

echo -e "\n${BLUE}=== Step 5: Removing project volumes ===${NC}"
VOLUMES=$(docker volume ls | grep -E "postgres-data|osint|domain-sleuth" | awk '{print $2}')
if [ -n "$VOLUMES" ]; then
    echo -e "${YELLOW}Removing project volumes...${NC}"
    for volume in $VOLUMES; do
        docker volume rm $volume || true
    done
    echo -e "${GREEN}Project volumes removed (if not in use).${NC}"
else
    echo -e "${GREEN}No project volumes found.${NC}"
fi

echo -e "\n${BLUE}=== Step 6: Running docker system prune ===${NC}"
echo -e "${YELLOW}Pruning unused Docker resources...${NC}"
docker system prune -f
echo -e "${GREEN}Docker system pruned.${NC}"

echo -e "\n${BLUE}=== Step 7: Pruning all volumes ===${NC}"
echo -e "${YELLOW}Pruning all unused volumes...${NC}"
docker volume prune -f
echo -e "${GREEN}All unused volumes removed.${NC}"

echo -e "\n${BLUE}=== Domain Sleuth Project Cleanup Complete ===${NC}"
echo -e "${YELLOW}Docker resources after cleanup:${NC}"
docker system df

echo -e "\n${GREEN}All Docker resources have been cleaned up!${NC}"
