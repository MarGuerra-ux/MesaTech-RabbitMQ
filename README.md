# MesaTech Cloud · RabbitMQ

**DSY1107 Desarrollo Cloud Native I · Evaluación Parcial 2 · Duoc UC · Sección 002V**
Integrantes: Marco Guerra · Jean Pierre Vidal · Daniel Vargas · Docente: Pablo Berrios

Integración de microservicios con **RabbitMQ** y **Amazon RDS** en AWS, más un **lanzador local con Microsoft Entra ID** para demostrar la mensajería en vivo.

## Contenido

| Carpeta | Qué es | Dónde corre |
|---|---|---|
| [`mesatech-ep2/`](mesatech-ep2/) | Proyecto EP2: BFF, Catálogo (publicador), Solicitudes (consumidor), frontend React, RabbitMQ con Docker Compose, scripts SQL, despliegue systemd y guion de pruebas P1–P8 | AWS: 3 EC2 + 2 RDS |
| [`lanzador/`](lanzador/) | Proyecto EP1 reciclado: login con Entra ID y disparo de solicitudes (individual o en modo demo) hacia la EP2, con log en tiempo real | Equipo local |

## Arquitectura

```
React (MSAL) ── Entra ID ── API Gateway ── BFF :8080 (EC2 A)
                                             ├─ Catálogo :8081 (EC2 A) ── RDS catalogo-db
                                             │        │ publica categoria.creada (confirm ACK)
                                             │        ▼
                                             │   RabbitMQ (EC2 C · Ubuntu 24.04 · Docker Compose)
                                             │   catalogo.exchange ─ categoria.creada ─▶ solicitudes.catalogo.queue
                                             │        │ consume (ack tras persistir)
                                             └─ Solicitudes :8082 (EC2 B) ── RDS solicitudes-db

Lanzador local (React + Spring Boot con Entra ID) ──▶ Catálogo (EC2 A) / Solicitudes (EC2 B)
```

## Versiones

Java 17 · Spring Boot 4.1.1 · Spring AMQP 4.1.1 · RabbitMQ 4.1.8 · Docker 29.9.3 · Compose v5.7.0 · MySQL 8.4 (RDS) · React 19 · MSAL 5

## Inicio rápido

- Despliegue en AWS: [`mesatech-ep2/README.md`](mesatech-ep2/README.md)
- Broker RabbitMQ: [`mesatech-ep2/rabbitmq/README.md`](mesatech-ep2/rabbitmq/README.md)
- Pruebas P1–P8: [`mesatech-ep2/docs/PRUEBAS.md`](mesatech-ep2/docs/PRUEBAS.md)
- Lanzador local: [`lanzador/README.md`](lanzador/README.md)

## Seguridad

Las credenciales reales viven solo en archivos `.env` (ignorados por git); el repositorio incluye únicamente `.env.example` con valores ficticios. Ninguna llave `.pem` se versiona.
