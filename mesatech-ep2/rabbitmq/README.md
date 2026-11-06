# MesaTech Cloud · EP2 · Broker RabbitMQ (EC2 C)

Instalación desde cero de RabbitMQ con Docker Compose en una EC2 Ubuntu 24.04 LTS sin broker preconfigurado.

## Recursos de mensajería

| Recurso | Nombre | Configuración |
|---|---|---|
| Virtual host | `mesatech` | Aísla los recursos del proyecto |
| Exchange | `catalogo.exchange` | direct, durable |
| Cola | `solicitudes.catalogo.queue` | durable, sin autoeliminación |
| Binding / routing key | `categoria.creada` | exchange → cola |
| Usuario admin | `mesatech_admin` (en `.env`) | tag administrator, solo interfaz de gestión |
| Usuario publicador | `catalogo_publisher` | configure `^$` · write `^catalogo\.exchange$` · read `^$` |
| Usuario consumidor | `solicitudes_consumer` | configure `^$` · write `^$` · read `^solicitudes\.catalogo\.queue$` |

**Por qué estos permisos:** cada aplicación tiene solo lo mínimo. El publicador puede escribir en el exchange, pero no crear, borrar ni leer colas. El consumidor solo puede leer su cola. Ninguno tiene tags de administración y ninguno usa `guest`.

> Como los usuarios de aplicación no tienen permiso *configure*, los microservicios **no deben declarar** exchange, cola ni binding como `@Bean` (RabbitAdmin intentaría crearlos y fallaría con ACCESS_REFUSED). Los recursos se crean con `setup-mesatech.sh`.

## Pasos (registrar captura de cada uno)

```bash
# 0. Evidencia de EC2 limpia (sin Docker ni broker)
lsb_release -a
docker --version            # debe decir "command not found"

# 1. Instalar Docker Engine y Compose desde el repositorio oficial
chmod +x install-docker-ubuntu.sh setup-mesatech.sh
./install-docker-ubuntu.sh
exit                         # volver a entrar por SSH para usar docker sin sudo

# 2. Credenciales fuera del repositorio
cp .env.example .env && chmod 600 .env
nano .env                    # poner claves reales

# 3. Validar, descargar, arrancar y revisar
docker compose config --quiet && echo "compose.yaml válido"
docker compose pull
docker compose up -d
docker compose ps
docker compose logs --tail=100 rabbitmq

# 4. Diagnóstico del broker
docker compose exec rabbitmq rabbitmq-diagnostics status
docker compose exec rabbitmq rabbitmq-diagnostics listeners      # 5672 y 15672
docker port mesatech-rabbitmq                                     # mapeo de puertos del contenedor
docker volume inspect mesatech_rabbitmq_data

# 5. Crear vhost, usuarios, permisos, exchange, cola y binding
./setup-mesatech.sh
```

Interfaz de gestión: `http://<IP-pública-EC2-C>:15672`, accesible solo desde la IP administrativa /32 del security group.

## Prueba P5: reinicio del broker conservando el volumen

Con el consumidor detenido y un mensaje en Ready:

```bash
docker compose exec rabbitmq rabbitmqctl list_queues -p mesatech name messages_ready messages_unacknowledged
docker compose down          # SIN -v: el volumen se conserva
docker compose up -d
docker compose exec rabbitmq rabbitmqctl list_vhosts
docker compose exec rabbitmq rabbitmqctl list_exchanges -p mesatech name type durable
docker compose exec rabbitmq rabbitmqctl list_queues -p mesatech name messages_ready
```

El mensaje sobrevive porque se juntan tres cosas: la cola es durable, el mensaje se publicó como persistente (delivery mode 2) y el volumen y el hostname son los mismos.

**Nunca** usar `docker compose down -v` durante las pruebas, porque borra el volumen.

## Prueba P7: fallo de publicación

```bash
docker compose stop rabbitmq     # crear categoría desde React → respuesta de error controlada
docker compose start rabbitmq
```

## Firewall del sistema operativo

```bash
sudo ufw status        # En Ubuntu de AWS suele estar "inactive"; el control lo hace el security group
```

Si `ufw` está activo, permitir 5672 solo desde la red privada (VPC) y 15672 y 22 solo desde la IP administrativa.

## Inspección de mensajes en la interfaz

En **Queues → solicitudes.catalogo.queue → Get messages**, usar el modo **"Nack message requeue true"**, que devuelve el mensaje a la cola. Nunca usar "Ack message requeue false" en las pruebas P3 a P5, porque retira el mensaje.
