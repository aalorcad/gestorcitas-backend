#!/bin/bash
# User data para una instancia EC2 Amazon Linux 2023: instala Docker, Compose y Buildx.
# (Compose v2.26+ necesita buildx 0.17+ para "docker compose up --build".)
# Luego: clonar el repo, copiar el .env y ejecutar el compose de esa instancia:
#   EC2-A (BFF):             docker compose -f docker-compose.ec2-bff.yml up -d --build
#   EC2-B (microservicios):  docker compose -f docker-compose.ec2-ms.yml  up -d --build
set -eux
dnf update -y
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user
mkdir -p /usr/local/lib/docker/cli-plugins

# Docker Compose
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Docker Buildx
case "$(uname -m)" in x86_64) BX_ARCH=amd64 ;; aarch64) BX_ARCH=arm64 ;; esac
BX=$(curl -s https://api.github.com/repos/docker/buildx/releases/latest | grep -m1 '"tag_name"' | cut -d'"' -f4)
curl -SL "https://github.com/docker/buildx/releases/download/${BX}/buildx-${BX}.linux-${BX_ARCH}" \
  -o /usr/local/lib/docker/cli-plugins/docker-buildx
chmod +x /usr/local/lib/docker/cli-plugins/docker-buildx
