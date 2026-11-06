#!/usr/bin/env bash
# Preparar EC2 A (BFF + Catálogo) y EC2 B (Solicitudes) en Ubuntu 24.04 LTS.
# Instala Java 17 (runtime), cliente MySQL para consultar RDS y crea /opt/mesatech.
set -euo pipefail

sudo apt-get update
sudo apt-get install -y openjdk-17-jre-headless mysql-client-core-8.0 || sudo apt-get install -y openjdk-17-jre-headless mysql-client
sudo mkdir -p /opt/mesatech
sudo chown -R "$USER":"$USER" /opt/mesatech

# Evidencia de versiones
lsb_release -a
java -version
mysql --version
echo "Instance ID: $(TOKEN=$(curl -sX PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60'); curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)"
hostname -I
