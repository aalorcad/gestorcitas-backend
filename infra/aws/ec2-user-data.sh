#!/bin/bash
# User data para una instancia EC2 Amazon Linux 2023: instala Docker + Compose.
# Luego: clonar el repo, crear .env y ejecutar "docker compose up -d --build".
set -eux
dnf update -y
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)" \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
