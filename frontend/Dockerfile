# Frontend Dockerfile
# Build stage
FROM node:20-alpine AS build

# Set working directory
WORKDIR /app

# Copy package files and install dependencies
COPY package*.json ./
RUN npm ci

# Copy source code
COPY . .

# Build the application
RUN npm run build

# Production stage
FROM node:20-alpine

# Set working directory
WORKDIR /app

# Copy build artifacts from build stage
COPY --from=build /app/dist /app/dist
COPY --from=build /app/package.json /app/
COPY --from=build /app/node_modules /app/node_modules

# Expose the port the app runs on
EXPOSE 5173

# Command to run the app
CMD ["npm", "run", "preview", "--", "--host", "0.0.0.0", "--port", "5173"]
