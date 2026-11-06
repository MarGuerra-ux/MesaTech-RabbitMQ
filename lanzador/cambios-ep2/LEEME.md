# Cambio en el proyecto EP2 (Solicitudes): demora configurable del consumidor

**Ya aplicado** en `mesatech-ep2/solicitudes-service/`. Esta carpeta queda solo como referencia del cambio.

Archivos modificados:
- `src/main/java/cl/duoc/mesatech/solicitudes/messaging/CategoriaCreadaListener.java`
- `src/main/resources/application.properties` (`mesatech.consumer.delay-ms=${CONSUMER_DELAY_MS:0}`)
- `.env.example` (`CONSUMER_DELAY_MS=0`)

## Activarlo en la EC2 B

```powershell
cd mesatech-ep2\solicitudes-service
.\mvnw.cmd clean package -DskipTests
scp -i <ruta>\Eva2.pem target\solicitudes-service.jar ec2-user@<IP-PUBLICA-B>:/opt/mesatech/solicitudes/
```
```bash
# EC2 B
grep -q CONSUMER_DELAY_MS /opt/mesatech/solicitudes/.env || echo "CONSUMER_DELAY_MS=0" >> /opt/mesatech/solicitudes/.env
sudo systemctl restart mesatech-solicitudes
journalctl -u mesatech-solicitudes --no-pager | grep "demora por mensaje" | tail -1
```

Con `CONSUMER_DELAY_MS=0` el comportamiento es idéntico al original. Para la demo, por ejemplo `1500`, y reiniciar el servicio.
