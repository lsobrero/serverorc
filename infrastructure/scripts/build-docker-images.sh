#!/bin/bash

# Script to build Docker images for services

# Exit immediately if a command exits with a non-zero status
set -e

# Check if service name is provided
if [ -z "$1" ]; then
    echo "Error: Service name is required. Use 'rest-customers' or 'rest-locations'"
    echo "Usage: $0 <service-name> [tag] [--build|--push]"
    exit 1
fi

# Define variables
IMAGE_NAME="$1"
TAG="${2:-latest}" # Use provided tag or default to "latest" if not provided

# Validate service name
if [ "$IMAGE_NAME" != "rest-customers" ] && [ "$IMAGE_NAME" != "rest-locations" ]; then
    echo "Error: Invalid service name. Use 'rest-customers' or 'rest-locations'"
    exit 1
fi

DOCKER_FILE_PATH="./src/main/docker/Dockerfile.jvm"
CONTEXT_PATH="."

# Navigate to the project root directory of the specified service
cd "$(dirname "$0")/../../$IMAGE_NAME/"

echo "Building $IMAGE_NAME:$TAG Docker image..."

# Check if Maven build is required
if [[ "$*" == *"--build"* ]] || [[ "$*" == *"-b"* ]]; then
    echo "Running Maven build first..."
    ./mvnw clean package -DskipTests
fi

# Build the Docker image
docker build -t "app/$IMAGE_NAME:$TAG" \
    -f "$DOCKER_FILE_PATH" \
    "$CONTEXT_PATH"

echo "Docker image $IMAGE_NAME:$TAG built successfully."

# Optional: Push to registry if --push flag is provided
if [[ "$*" == *"--push"* ]]; then
    echo "Pushing image to registry..."
    docker push "$IMAGE_NAME:$TAG"
    echo "Image pushed successfully."
fi

echo "Done."