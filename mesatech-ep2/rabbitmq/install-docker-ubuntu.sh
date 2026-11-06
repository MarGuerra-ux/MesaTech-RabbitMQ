#!/usr/bin/env bash
# Instalación desde cero de Docker Engine + plugin Docker Compose en Ubuntu 24.04 LTS (EC2 C)
# Fuente oficial: https://docs.docker.com/engine/install/ubuntu/
set -euo pipefail

# 1. Paquetes previos y llave GPG oficial de Docker
sudo apt-get update
sudo apt-get install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

# 2. Repositorio oficial de Docker
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 3. Docker Engine, CLI, containerd y plugin Compose
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 4. Usar docker sin sudo (requiere cerrar y abrir la sesión SSH)
sudo usermod -aG docker "$USER"
sudo systemctl enable --now docker

# 5. Evidencia de versiones
lsb_release -a
docker --version
docker compose version
sudo docker run --rm hello-world
