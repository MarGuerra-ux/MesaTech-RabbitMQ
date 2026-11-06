package cl.duoc.mesatech.bff.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Reenvía las peticiones ya autorizadas hacia Catálogo y Solicitudes.
 * El BFF no accede a RDS ni publica/consume mensajes: solo coordina.
 * Respeta el código HTTP del microservicio (ej. 503 controlado de Catálogo en P7).
 */
@Component
public class MicroservicioClient {

	private static final Logger log = LoggerFactory.getLogger(MicroservicioClient.class);

	private final RestClient catalogo;
	private final RestClient solicitudes;

	public MicroservicioClient(@Value("${mesatech.catalogo.url}") String catalogoUrl,
			@Value("${mesatech.solicitudes.url}") String solicitudesUrl) {
		this.catalogo = RestClient.create(catalogoUrl);
		this.solicitudes = RestClient.create(solicitudesUrl);
	}

	public ResponseEntity<String> catalogo(HttpMethod metodo, String ruta, String cuerpo, String usuario) {
		return reenviar("catalogo", catalogo, metodo, ruta, cuerpo, usuario);
	}

	public ResponseEntity<String> solicitudes(HttpMethod metodo, String ruta, String cuerpo, String usuario) {
		return reenviar("solicitudes", solicitudes, metodo, ruta, cuerpo, usuario);
	}

	private ResponseEntity<String> reenviar(String servicio, RestClient cliente, HttpMethod metodo,
			String ruta, String cuerpo, String usuario) {
		try {
			RestClient.RequestBodySpec peticion = cliente.method(metodo).uri(ruta)
					.header("X-Usuario", usuario == null ? "" : usuario)
					.accept(MediaType.APPLICATION_JSON);
			if (cuerpo != null) {
				peticion = peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
			}
			ResponseEntity<String> r = peticion.retrieve().toEntity(String.class);
			return ResponseEntity.status(r.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(r.getBody());
		} catch (RestClientResponseException e) {
			return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON)
					.body(e.getResponseBodyAsString());
		} catch (ResourceAccessException e) {
			log.error("[BFF] {} no disponible: {}", servicio, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
					.body("{\"error\":\"SERVICIO_NO_DISPONIBLE\",\"mensaje\":\"El servicio " + servicio
							+ " no responde\"}");
		}
	}
}
