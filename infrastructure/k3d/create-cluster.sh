# This script creates a simple k3d cluster with a registry proxy
# and mounts the Docker socket for local image access.
# It uses a temporary config file to avoid cluttering the current directory.
# from https://github.com/ligfx/k3d-registry-dockerd

# Set default values
cluster_name=${1:-cluster-00}
agents=${2:-0}

# Confirm default values with the user
read -p "Cluster name [$cluster_name]: " input_cluster_name
cluster_name=${input_cluster_name:-$cluster_name}

read -p "Number of agent nodes [$agents]: " input_agents
agents=${input_agents:-$agents}

echo "Creating K3d cluster '$cluster_name' with $agents agent nodes..."

configfile=$(mktemp)
cat << HERE > "$configfile"
apiVersion: k3d.io/v1alpha5
kind: Simple
servers: 1 
agents: $agents  

image: rancher/k3s:v1.29.3-k3s1 

options:
  k3d:
    wait: true
    timeout: "360s"

  k3s: # Add this section
    extraArgs: # Add this
      - arg: "--disable=traefik" # Add this
        nodeFilters: # Add this
          - server:* # Add this

  kubeconfig:
    updateDefaultKubeconfig: true
    switchCurrentContext: true
    
registries:
  create:
    image: ligfx/k3d-registry-dockerd:v0.8
    proxy:
      remoteURL: "*"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
HERE
k3d cluster create "$cluster_name" --config "$configfile"
rm "$configfile"