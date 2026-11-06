# MesaTech Cloud · EP2 · DSY1107 Desarrollo Cloud Native I

Dos microservicios Spring Boot en EC2 distintas, cada uno con su propia instancia Amazon RDS, integrados mediante RabbitMQ instalado desde cero con Docker Compose en una tercera EC2.

## Arquitectura

```
React (MSAL) ──login──> Microsoft Entra ID
   │  Bearer JWT
   ▼
AWS API Gateway ──> BFF :8080 (EC2 A) ── valida JWT (scope + rol Admin)
                      │ localhost:8081            │ IP privada :8082
                      ▼                           ▼
              Catálogo (EC2 A)            Solicitudes (EC2 B)
              PUBLICADOR                  CONSUMIDOR
                │ JDBC :3306                │ JDBC :3306
                ▼                           ▼
        RDS mesatech-rds-catalogo   RDS mesatech-rds-solicitudes
        (propietario: Catálogo)     (propietario: Solicitudes)

Catálogo ──AMQP :5672──> RabbitMQ (EC2 C, Docker Compose)
   vhost mesatech: catalogo.exchange (direct) ──categoria.creada──> solicitudes.catalogo.queue
                                                                        │ AMQP :5672
                                                              Solicitudes (listener) ──> RDS Solicitudes
```

| Componente | Ubicación | Puerto | Carpeta |
|---|---|---|---|
| BFF | EC2 A | 8080 | `bff/` |
| Catálogo + publicador | EC2 A | 8081 | `catalogo-service/` |
| Solicitudes + consumidor | EC2 B | 8082 | `solicitudes-service/` |
| RabbitMQ | EC2 C (Ubuntu 24.04) | 5672 / 15672 | `rabbitmq/` |
| RDS Catálogo | Instancia exclusiva | 3306 | `sql/catalogo.sql` |
| RDS Solicitudes | Instancia exclusiva | 3306 | `sql/solicitudes.sql` |
| Frontend | Local (`npm start`) | 3000 | `frontend/` |

## Versiones

| Componente | Versión |
|---|---|
| Java | 17 (OpenJDK) |
| Spring Boot | 4.1.1 (Spring AMQP 4.1.1, Jackson 3) |
| Maven | 3.9.x (vía wrapper `mvnw`) |
| RabbitMQ | imagen `rabbitmq:4.1-management` |
| Docker / Compose | Docker Engine y plugin Compose del repositorio oficial (anotar versión de `docker --version` y `docker compose version`) |
| MySQL (RDS) | 8.0 |
| React | 19 + MSAL 5 + Axios |

## Contrato del evento `categoria.creada`

Exchange `catalogo.exchange` (direct, durable) · routing key `categoria.creada` · cola `solicitudes.catalogo.queue` (durable).
Mensaje persistente (delivery mode 2), `content_type=application/json`, `message_id = eventId`.

| Campo | Tipo | Formato |
|---|---|---|
| `eventId` | String | UUID v4 |
| `tipoEvento` | String | `CATEGORIA_CREADA` |
| `fechaEvento` | String | ISO-8601 UTC (`2026-11-05T23:10:42.123456Z`) |
| `categoriaId` | Long | ID original en RDS Catálogo |
| `nombre` | String | Nombre de la categoría |

```json
{"eventId":"3f1c2a9e-8b7d-4e21-9a0c-5d6e7f8a9b0c","tipoEvento":"CATEGORIA_CREADA","fechaEvento":"2026-11-05T23:10:42.123456Z","categoriaId":7,"nombre":"Soporte VPN"}
```

> Reemplazar este ejemplo por un mensaje real copiado del log `[PUBLICADO]` o de la interfaz de RabbitMQ.

## Endpoints (vía API Gateway → BFF)

| Método | Ruta | Permiso | Destino |
|---|---|---|---|
| GET | `/public/hola` | público | BFF |
| GET | `/api/usuario` | scope `access_as_user` | BFF (muestra claims y roles) |
| GET | `/api/v1/catalogo/categorias` | scope | Catálogo `GET /categorias` |
| GET | `/api/v1/catalogo/prioridades` | scope | Catálogo `GET /prioridades` |
| POST | `/api/v1/catalogo/categorias` | **rol Admin** | Catálogo `POST /categorias` → RDS + evento |
| PUT / DELETE | `/api/v1/catalogo/categorias/{id}` | rol Admin | Catálogo (sin evento, como en la EP1) |
| GET | `/api/v1/solicitudes/categorias` | scope | Solicitudes `GET /categorias` (copia local) |
| GET | `/api/v1/solicitudes` · `/api/v2/solicitudes` | scope | Solicitudes v1 (resumida) / v2 (extendida) |
| POST | `/api/v1/solicitudes` · `/api/v2/solicitudes` | scope | Solicitudes `POST /solicitudes` |
| PATCH | `/api/v{1,2}/solicitudes/{id}/estado` | rol Admin | Solicitudes (solo RESUELTA desde EN_PROCESO) |

Respuestas relevantes: `401` sin token · `403` sin rol Admin · `409` regla de estados o categoría duplicada · `422` categoría aún no sincronizada · `503 EVENTO_NO_CONFIRMADO` si RabbitMQ no confirma (la categoría **quedó guardada**).

## Requisitos previos en Entra ID (continuidad EP1)

1. App **api-cloud-native**: exponer la API con el ámbito `access_as_user` y token v2 (guía del profesor).
2. En **api-cloud-native → Roles de aplicación → Crear rol**: nombre `Admin`, tipos de miembro *Usuarios/Grupos*, valor `Admin`.
3. **Aplicaciones empresariales → api-cloud-native → Usuarios y grupos → Agregar**: asignar el rol `Admin` al usuario administrador. Dejar otro usuario **sin** rol para probar el 403 (P8).
4. App del frontend (SPA) con redirect `http://localhost:3000/` y permiso delegado a `access_as_user`.

## Compilación (en el computador, terminal de VS Code)

Requiere JDK 17 o superior. El wrapper descarga Maven automáticamente.

```bash
cd catalogo-service    && ./mvnw clean package -DskipTests && cd ..
cd solicitudes-service && ./mvnw clean package -DskipTests && cd ..
cd bff                 && ./mvnw clean package -DskipTests && cd ..
# En Windows PowerShell: .\mvnw.cmd clean package -DskipTests
```

Genera `target/catalogo-service.jar`, `target/solicitudes-service.jar` y `target/bff.jar`.

## Despliegue

### 1. EC2 C · RabbitMQ
Ver [`rabbitmq/README.md`](rabbitmq/README.md): instalar Docker, `.env`, `docker compose up -d` y `./setup-mesatech.sh`.

### 2. EC2 A y EC2 B · Java y cliente MySQL
```bash
scp -i labsuser.pem deploy/install-java-ec2.sh ubuntu@<IP-EC2>:~
ssh -i labsuser.pem ubuntu@<IP-EC2> 'bash install-java-ec2.sh'
```

### 3. Crear tablas en cada RDS (desde la EC2 dueña)
```bash
# EC2 A
CLAVE_APP='clave_real'; sed "s/__CLAVE_APP__/$CLAVE_APP/" catalogo.sql | mysql -h <endpoint-rds-catalogo> -u admin -p
# EC2 B
CLAVE_APP='clave_real'; sed "s/__CLAVE_APP__/$CLAVE_APP/" solicitudes.sql | mysql -h <endpoint-rds-solicitudes> -u admin -p
```

### 4. Copiar jars, `.env` y servicios
```bash
# EC2 A
ssh ubuntu@<IP-A> 'mkdir -p /opt/mesatech/catalogo /opt/mesatech/bff'
scp catalogo-service/target/catalogo-service.jar ubuntu@<IP-A>:/opt/mesatech/catalogo/
scp bff/target/bff.jar ubuntu@<IP-A>:/opt/mesatech/bff/
scp deploy/systemd/mesatech-catalogo.service deploy/systemd/mesatech-bff.service ubuntu@<IP-A>:~
# EC2 B
ssh ubuntu@<IP-B> 'mkdir -p /opt/mesatech/solicitudes'
scp solicitudes-service/target/solicitudes-service.jar ubuntu@<IP-B>:/opt/mesatech/solicitudes/
scp deploy/systemd/mesatech-solicitudes.service ubuntu@<IP-B>:~
```
En cada EC2, crear `/opt/mesatech/<servicio>/.env` a partir del `.env.example` correspondiente (`chmod 600`), y luego:
```bash
sudo mv ~/mesatech-*.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now mesatech-catalogo mesatech-bff     # EC2 A
sudo systemctl enable --now mesatech-solicitudes               # EC2 B
journalctl -u mesatech-catalogo -f                             # logs
```

### 5. API Gateway
HTTP API con integración HTTP proxy hacia `http://<IP-pública-o-Elastic-IP-EC2-A>:8080/{proxy}` (ruta `ANY /{proxy+}`). Si cambia la IP de la EC2 A, actualizar la integración.

### 6. Frontend
```bash
cd frontend
cp .env.example .env     # completar IDs de Entra ID y URL de API Gateway
npm install
npm start                # http://localhost:3000
```

## Arranque, detención y orden de recuperación

**Arranque:** 1) RDS disponibles → 2) RabbitMQ (`docker compose up -d`) → 3) Catálogo → 4) BFF → 5) Solicitudes → 6) Frontend.
**Detención:** inverso (frontend, Solicitudes, BFF, Catálogo, RabbitMQ con `docker compose stop`). Nunca `docker compose down -v`.
Los servicios systemd y la política `restart: unless-stopped` de RabbitMQ vuelven a levantar todo si se reinicia una EC2. Si Solicitudes arranca antes que RabbitMQ, reintenta la conexión solo.

## Seguridad y credenciales

- Archivos `.env` reales fuera del repositorio (`.gitignore`). Solo se entregan `.env.example` con valores ficticios.
- Usuarios RabbitMQ separados y sin privilegios de administración: `catalogo_publisher` (solo escribe en el exchange) y `solicitudes_consumer` (solo lee la cola). No se usa `guest`.
- Usuarios MySQL de aplicación con permisos DML solo sobre su base y solo desde la VPC (`10.0.%`).
- RDS sin acceso público; 3306 abierto solo al security group de la EC2 propietaria.

## Garantías y límites (para la defensa)

- El **confirm del publicador** indica que RabbitMQ aceptó el mensaje; **no** acredita que Solicitudes lo guardó.
- El **ack del consumidor** (modo AUTO) se envía solo después de guardar en RDS Solicitudes.
- Guardar en RDS y publicar son operaciones separadas: si la publicación falla, la categoría queda con `estado_publicacion = ERROR_PUBLICACION` y la API responde `503`. No hay rollback automático ni outbox.
- Reenviar la solicitud HTTP crea otra categoría o choca con el nombre único (409). Si se reentrega un mensaje, la copia local se actualiza por `categoria_id` (no se duplica), pero **no** se garantiza procesamiento exactamente una vez.

Las pruebas P1 a P8 están en [`docs/PRUEBAS.md`](docs/PRUEBAS.md).
