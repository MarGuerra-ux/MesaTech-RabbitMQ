# Guion de pruebas P1 a P8 (evidencias y demo en vivo)

Para cada prueba, registrar en el informe: **preparación, datos usados, resultado esperado, resultado obtenido y evidencia** (captura con explicación).
Usar nombres de categoría fáciles de seguir: `P2-Redes`, `P3-Impresoras`, `P5-Correo`, `P7-Telefonia`.

Terminales sugeridas durante la demo:
- **T-A** (EC2 A): `journalctl -u mesatech-catalogo -f`
- **T-B** (EC2 B): `journalctl -u mesatech-solicitudes -f`
- **T-C** (EC2 C): carpeta `rabbitmq/`
- Navegador: React (`localhost:3000`) + RabbitMQ (`http://<IP-C>:15672` → Queues)

Comando de estado de la cola (T-C):
```bash
docker compose exec rabbitmq rabbitmqctl list_queues -p mesatech name messages_ready messages_unacknowledged consumers
```

---

## P1 · Separación real
```bash
# En EC2 A, B y C:
TOKEN=$(curl -sX PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')
curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id; echo
hostname -I
sudo ss -ltnp | grep -E '8080|8081|8082|5672|15672'
# Conexión de cada proceso a su RDS:
grep DB_URL /opt/mesatech/*/.env
```
Consola AWS: lista de EC2 (IDs, nombres, IPs privadas) y RDS → Databases (dos identificadores y endpoints distintos).
Consultar `SELECT @@hostname, DATABASE();` en cada RDS desde su EC2.

## P2 · Flujo completo
1. React con usuario **Admin** → crear `P2-Redes`.
2. T-A: `[GUARDADO RDS CATALOGO]` y `[PUBLICADO] ... confirmacion=ACK`.
3. T-B: `[RECIBIDO] eventId=...` y `[PERSISTIDO RDS SOLICITUDES]`.
4. RDS Catálogo: `SELECT * FROM categoria WHERE nombre='P2-Redes';`
5. RDS Solicitudes: `SELECT * FROM categoria_local WHERE nombre='P2-Redes';` → mismo `categoria_id` y `event_id`.
6. React: la categoría pasa a **Disponible** → registrar una solicitud con ella → `SELECT * FROM solicitud ORDER BY id DESC LIMIT 1;`

## P3 · Consumidor detenido
```bash
sudo systemctl stop mesatech-solicitudes      # EC2 B
```
Crear `P3-Impresoras` en React. Esperado: está en RDS Catálogo; la cola muestra **Ready = 1** y **consumers = 0**; no existe en `categoria_local`; React la muestra como *Pendiente de sincronizar*.
Si se inspecciona en la interfaz: **Get messages** con **Ack mode = "Nack message requeue true"**.

## P4 · Consumo diferido
```bash
sudo systemctl start mesatech-solicitudes     # EC2 B
```
Esperado: T-B muestra `[RECIBIDO]` + `[PERSISTIDO]` del eventId de P3; la cola queda con **Ready 0 / Unacked 0**; la fila existe en `categoria_local`. (La cola vacía sola no basta: mostrar la fila en RDS.)

## P5 · Reinicio del broker
```bash
sudo systemctl stop mesatech-solicitudes      # EC2 B
# Crear P5-Correo en React; verificar Ready = 1 (T-C)
docker compose down                           # T-C (SIN -v)
docker compose up -d
docker compose ps
docker compose exec rabbitmq rabbitmqctl list_vhosts
docker compose exec rabbitmq rabbitmqctl list_exchanges -p mesatech name type durable
docker compose exec rabbitmq rabbitmqctl list_bindings -p mesatech source_name destination_name routing_key
docker compose exec rabbitmq rabbitmqctl list_queues -p mesatech name messages_ready
sudo systemctl start mesatech-solicitudes     # EC2 B → verificar escritura en categoria_local
```
Explicar: persiste porque la cola es durable, el mensaje es persistente y el volumen y el hostname se conservan.

## P6 · Reinicio de aplicaciones
```bash
sudo systemctl restart mesatech-catalogo mesatech-bff   # EC2 A
sudo systemctl restart mesatech-solicitudes             # EC2 B
```
Volver a listar categorías y solicitudes en React y con SQL: los registros de P2 a P5 siguen en ambos RDS.

## P7 · Fallo de publicación
```bash
docker compose stop rabbitmq                  # T-C
```
Crear `P7-Telefonia`. Esperado: React muestra la advertencia **HTTP 503 EVENTO_NO_CONFIRMADO** con categoriaId y eventId; T-A muestra `[ERROR PUBLICACION]`.
RDS Catálogo: `SELECT id, nombre, estado_publicacion FROM categoria WHERE nombre='P7-Telefonia';` → existe con `ERROR_PUBLICACION`.
```bash
docker compose start rabbitmq
```
Explicar la inconsistencia: la categoría existe en Catálogo pero nunca llegará a Solicitudes, porque no hay outbox ni reintento automático. Queda *Pendiente de sincronizar*. Reenviar el formulario daría `409` por nombre duplicado.

## P8 · Continuidad EP1
| Caso | Cómo | Esperado |
|---|---|---|
| Login y consumo autorizado | React con usuario Admin | Listas cargan (200) |
| Sin token | `curl -i https://<api-gateway>/api/v1/catalogo/categorias` | **401** |
| Permisos insuficientes | Usuario **sin rol Admin** crea categoría o cambia estado | **403** |
| Regla de estados | Solicitud ABIERTA → botón RESUELTA | **409** "Solo se puede resolver… EN_PROCESO" |
| v1 y v2 | Botones API v1 / API v2 | v1 resumida, v2 con nombre de categoría y solicitante |
