# MesaTech · Lanzador local con Microsoft Entra ID

Proyecto reciclado de la EP1 (MSProyect), reducido a dos funciones:

1. **Autenticar con Microsoft Entra ID** y validar credenciales y rol (Entra ID **habilitado**).
2. **Disparar solicitudes** hacia el proyecto EP2 en AWS para ver trabajar RabbitMQ en vivo.

No tiene microservicios ni base de datos propios, y no crea instancias nuevas: lee y escribe sobre la EP2.

```
PC local
 ├─ lanzador-web  · React + MSAL · http://localhost:3000   (botones, modo demo, log en tiempo real)
 └─ lanzador-api  · Spring Boot 4.1.1 · http://localhost:8080
       • valida el JWT de Entra ID (firma, emisor, audiencia, scope access_as_user)
       • exige el App Role Admin para disparar (sin rol -> 403, sin token -> 401)
       • POST /api/disparar             -> EC2 A Catálogo :8081  POST /categorias  (RDS + RabbitMQ)
       • GET  /api/catalogo/categorias  -> EC2 A Catálogo :8081  (estado de publicación)
       • GET  /api/solicitudes/categorias -> EC2 B Solicitudes :8082 (copia local = consumidas)
```

Cada "solicitud" disparada **crea una categoría** (`Demo-HHmmss-0001`, …), porque `categoria.creada` es el evento que recorre RabbitMQ:
Catálogo guarda en RDS → publica con ACK → cola `solicitudes.catalogo.queue` → Solicitudes consume → copia en `categoria_local`.

**Sin CORS:** React llama a `localhost:8080` a través del proxy de desarrollo (`"proxy"` en `package.json`), es decir, desde el mismo origen. CORS solo existe en el BFF de AWS.

---

## 1. Entra ID (una vez)

En **portal.azure.com → Microsoft Entra ID → Registros de aplicaciones**:

**API (`api-cloud-native`, de la EP1)**
- *Exponer una API*: debe existir el ámbito `access_as_user`.
- *Manifiesto*: `"requestedAccessTokenVersion": 2` (token v2.0).
- *Roles de aplicación → Crear rol de aplicación*: nombre visible `Admin`, tipos de miembro **Usuarios/Grupos**, valor **`Admin`**, habilitado.
- Anotar el **Id. de aplicación (cliente)** y el **Id. de directorio (inquilino)**.

**Asignar el rol**: *Aplicaciones empresariales → api-cloud-native → Usuarios y grupos → Agregar usuario o grupo* → su usuario → rol **Admin**.

**SPA del frontend** (si no existe): *Nuevo registro* → nombre `mesatech-lanzador-web` → *Autenticación → Agregar plataforma → Aplicación de página única* → URI `http://localhost:3000/`.
*Permisos de API → Agregar permiso → Mis API → api-cloud-native → access_as_user* → **Conceder consentimiento de administrador**.

**Prueba del 403:** necesita un usuario **sin** el rol Admin. Puede crear un segundo usuario en el tenant, o quitar temporalmente la asignación del rol, cerrar sesión, probar y volver a asignarlo. El rol viaja dentro del token: después de cambiarlo hay que **cerrar sesión e iniciarla de nuevo**.

## 2. Red en AWS (EP2)

Agregar dos reglas de entrada, **solo para su IP pública /32** (se obtiene con `curl.exe -s https://checkip.amazonaws.com`):

| Grupo de seguridad | Puerto | Origen | Uso |
|---|---|---|---|
| EC2 A (Catálogo) | 8081 | `<su IP>/32` | El lanzador dispara y lee Catálogo |
| EC2 B (Solicitudes) | 8082 | `<su IP>/32` | El lanzador lee la copia local |

Si cambia de red (casa ↔ sede), actualice la IP de estas reglas.

## 3. Ejecutar (terminal de VS Code)

**Backend:**
```powershell
cd lanzador-api
copy .env.example .env      # completar tenant, Id. de la API e IP públicas actuales de EC2 A y B
.\mvnw.cmd spring-boot:run
```
Log esperado: `[LANZADOR] Catálogo EP2 = http://<IP-A>:8081 · Solicitudes EP2 = http://<IP-B>:8082`.

**Frontend** (otra terminal):
```powershell
cd lanzador-web
copy .env.example .env      # completar Id. de la SPA, tenant, scope y URL del panel RabbitMQ
npm install
npm start                   # http://localhost:3000
```

Las IP públicas cambian al reiniciar el Learner Lab: actualice `CATALOGO_URL` y `SOLICITUDES_URL` en `lanzador-api/.env` y reinicie el backend.

## 4. Demora del consumidor (EP2, EC2 B)

Para que la cola se vea **crecer y vaciarse**, Solicitudes acepta una demora por mensaje. Se ajusta manualmente:

```bash
# EC2 B
sed -i 's|^CONSUMER_DELAY_MS=.*|CONSUMER_DELAY_MS=1500|' /opt/mesatech/solicitudes/.env || echo "CONSUMER_DELAY_MS=1500" >> /opt/mesatech/solicitudes/.env
sudo systemctl restart mesatech-solicitudes
journalctl -u mesatech-solicitudes --no-pager | grep "demora por mensaje" | tail -1
```

`CONSUMER_DELAY_MS=0` vuelve al comportamiento normal. Requiere el jar actualizado de `solicitudes-service` (carpeta `cambios-ep2/`).

**Ejemplo de demo:** 20 envíos cada 300 ms con demora de 1500 ms → en el panel de RabbitMQ, Ready sube hasta unos 15 y luego baja de a uno; el log del lanzador pasa de *En cola…* a *Consumida*.

## 5. Qué mirar durante la demo

- **Lanzador:** contadores (enviadas, ACK, errores, consumidas y en cola) y log con hora, HTTP, `categoriaId`, `eventId`, publicación y consumo.
- **RabbitMQ** `http://<IP-C>:15672` → cola `solicitudes.catalogo.queue`: Ready, Unacked, Consumers y gráfico de tasas (*last ten minutes*).
- **EC2 A:** `journalctl -u mesatech-catalogo -f | grep -E "GUARDADO|PUBLICADO"`
- **EC2 B:** `journalctl -u mesatech-solicitudes -f | grep -E "RECIBIDO|PERSISTIDO"`

## 6. Respuestas esperadas (P8)

| Caso | Resultado |
|---|---|
| Usuario con rol Admin dispara | 201 · `PUBLICADA` |
| Petición sin token (`curl -i -X POST http://localhost:8080/api/disparar`) | **401** |
| Usuario sin rol Admin dispara | **403** · log `[DISPARO RECHAZADO]` |
| Broker detenido (`docker compose stop rabbitmq`) | 503 · `ERROR_PUBLICACION` (P7) |

## Seguridad

- `.env` fuera del repositorio; solo se versiona `.env.example`.
- Se eliminó del proyecto reciclado la carpeta `AWS-Keys` con la llave `.pem` de la EP1. **Nunca suba llaves privadas a un repositorio**; si estuvo en GitHub, considérela comprometida: elimine el par de llaves en la consola EC2 y cree uno nuevo.
- Los puertos 8081 y 8082 se abren solo a la IP /32 del presentador, y el lanzador exige token y rol antes de llamar a la EP2.
