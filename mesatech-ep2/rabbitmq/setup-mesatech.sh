#!/usr/bin/env bash
# MesaTech Cloud - EP2
# Crea los recursos de mensajería de forma reproducible:
#   virtual host, usuarios de aplicación, permisos, exchange direct durable,
#   cola durable sin autoeliminación y binding.
# Uso (en la EC2 C, dentro de la carpeta rabbitmq, con el contenedor arriba):
#   ./setup-mesatech.sh
# Es idempotente: se puede ejecutar varias veces.
set -euo pipefail

cd "$(dirname "$0")"
[ -f .env ] || { echo "Falta .env (copiar desde .env.example)"; exit 1; }
set -a; source .env; set +a

VHOST="mesatech"
EXCHANGE="catalogo.exchange"
QUEUE="solicitudes.catalogo.queue"
ROUTING_KEY="categoria.creada"
API="http://localhost:15672/api"
AUTH="${RABBITMQ_ADMIN_USER}:${RABBITMQ_ADMIN_PASS}"

rmq() { docker compose exec -T rabbitmq rabbitmqctl "$@"; }

echo "==> Esperando que el broker responda..."
until docker compose exec -T rabbitmq rabbitmq-diagnostics -q ping >/dev/null 2>&1; do sleep 3; done

echo "==> Virtual host: ${VHOST}"
rmq add_vhost "${VHOST}" 2>/dev/null || echo "   (ya existía)"

echo "==> Usuarios de aplicación (sin tags de administración)"
rmq add_user "${RABBITMQ_PUBLISHER_USER}" "${RABBITMQ_PUBLISHER_PASS}" 2>/dev/null \
  || rmq change_password "${RABBITMQ_PUBLISHER_USER}" "${RABBITMQ_PUBLISHER_PASS}"
rmq add_user "${RABBITMQ_CONSUMER_USER}" "${RABBITMQ_CONSUMER_PASS}" 2>/dev/null \
  || rmq change_password "${RABBITMQ_CONSUMER_USER}" "${RABBITMQ_CONSUMER_PASS}"
rmq set_user_tags "${RABBITMQ_PUBLISHER_USER}"
rmq set_user_tags "${RABBITMQ_CONSUMER_USER}"

echo "==> Permisos (configure / write / read) sobre ${VHOST}"
# Administrador: control total del vhost para operarlo desde la interfaz
rmq set_permissions -p "${VHOST}" "${RABBITMQ_ADMIN_USER}" ".*" ".*" ".*"
# Publicador: solo escribe en el exchange; no crea ni lee recursos
rmq set_permissions -p "${VHOST}" "${RABBITMQ_PUBLISHER_USER}" "^$" "^catalogo\.exchange$" "^$"
# Consumidor: solo lee de su cola; no crea recursos ni publica
rmq set_permissions -p "${VHOST}" "${RABBITMQ_CONSUMER_USER}" "^$" "^$" "^solicitudes\.catalogo\.queue$"

echo "==> Exchange direct durable: ${EXCHANGE}"
curl -fsS -u "${AUTH}" -H "content-type: application/json" -X PUT \
  "${API}/exchanges/${VHOST}/${EXCHANGE}" \
  -d '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

echo "==> Cola durable sin autoeliminación: ${QUEUE}"
curl -fsS -u "${AUTH}" -H "content-type: application/json" -X PUT \
  "${API}/queues/${VHOST}/${QUEUE}" \
  -d '{"durable":true,"auto_delete":false,"arguments":{}}'

echo "==> Binding ${EXCHANGE} --(${ROUTING_KEY})--> ${QUEUE}"
curl -fsS -u "${AUTH}" -H "content-type: application/json" -X POST \
  "${API}/bindings/${VHOST}/e/${EXCHANGE}/q/${QUEUE}" \
  -d "{\"routing_key\":\"${ROUTING_KEY}\",\"arguments\":{}}"

echo
echo "==> Verificación"
rmq list_vhosts
rmq list_users
rmq list_permissions -p "${VHOST}"
rmq list_exchanges -p "${VHOST}" name type durable auto_delete
rmq list_queues -p "${VHOST}" name durable auto_delete messages_ready messages_unacknowledged
rmq list_bindings -p "${VHOST}" source_name destination_name routing_key
echo "Listo."
