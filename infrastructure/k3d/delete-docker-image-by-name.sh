#!/bin/bash
# filepath: /home/luis/experiment/infrastructure/k3d/k3d-cluster/delete-docker-images.sh

# This script deletes Docker images with tags containing a specific name
# Usage: ./delete-docker-images.sh [name]
# If no name is provided, it will exit without deleting anything

# Get the name from command line argument or set to empty
name=${1:-none}


# Find images with tags containing the specified name
echo "Searching for Docker images with name containing '$name'..."
matching_images=$(docker images | grep "$name" | awk '{print $1":"$2":"$3}')

if [ -z "$matching_images" ]; then
  echo "No Docker images with name containing '$name' found."
  exit 0
fi

# Display the images that will be deleted
echo "The following images will be deleted:"
echo "$matching_images"

# Ask for confirmation
read -p "Do you want to proceed with deletion? (y/n): " confirm

if [[ "$confirm" == [yY] || "$confirm" == [yY][eE][sS] ]]; then
  # Delete the images
  echo "Deleting images..."
  
  for image in $matching_images; do
    # Extract just the first token (image ID) from the colon-separated string
    image_id=$(echo $image | cut -d':' -f3)
    echo "Removing $image_id..."
    docker rmi "$image_id" || echo "Failed to remove $image_id"
  done
  
  echo "Deletion completed."
else
  echo "Deletion cancelled."
fi