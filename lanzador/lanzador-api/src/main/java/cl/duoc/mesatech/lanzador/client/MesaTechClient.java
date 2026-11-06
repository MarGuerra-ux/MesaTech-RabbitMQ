package cl.duoc.mesatech.lanzador.client;

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
 * Cliente hacia los microservicios de la EP2 en AWS. El lanzador no tiene base de datos:
 * solo lee y dispara sobre Catálogo (EC2 A) y lee la copia local de Solicitudes (EC2 B).
 */
@Component
public class MesaTechClient {

	private static final Logger log = LoggerFactory.getLogger(MesaTechClient.class);

	private final RestClient catalogo;
	private final RestClient solicitudes;

	public MesaTechClient(@Value("${mesatech.catalogo.url}") String catalogoUrl,
			@Value("${mesatech.solicitudes.url}") String solicitudesUrl) {
		this.catalogo = RestClient.create(catalogoUrl);
		this.solicitudes = RestClient.create(solicitudesUrl);
		log.info("[LANZADOR] Catálogo EP2 = {} · Solicitudes EP2 = {}", catalogoUrl, solicitudesUrl);
	}

	public ResponseEntity<String> catalogo(HttpMethod metodo, String ruta, String cuerpo) {
		return llamar("Catálogo (EC2 A)", catalogo, metodo, ruta, cuerpo);
	}

	public ResponseEntity<String> solicitudes(HttpMethod metodo, String ruta) {
		return llamar("Solicitudes (EC2 B)", solicitudes, metodo, ruta, null);
	}

	private ResponseEntity<String> llamar(String servicio, RestClient cliente, HttpMethod metodo, String ruta, String cuerpo) {
		try {
			RestClient.RequestBodySpec peticion = cliente.method(metodo).uri(ruta).accept(MediaType.APPLICATION_JSON);
			if (cuerpo != null) {
				peticion = peticion.contentType(MediaType.APPLICATION_JSON).body(cuerpo);
			}
			ResponseEntity<String> r = peticion.retrieve().toEntity(String.class);
			return ResponseEntity.status(r.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(r.getBody());
		} catch (RestClientResponseException e) {
			// Respeta el código del microservicio (ej. 503 EVENTO_NO_CONFIRMADO de Catálogo)
			return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
		} catch (ResourceAccessException e) {
			log.error("[LANZADOR] {} no responde: {}", servicio, e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).contentType(MediaType.APPLICATION_JSON)
					.body("{\"error\":\"SERVICIO_NO_DISPONIBLE\",\"mensaje\":\"" + servicio
							+ " no responde. Revise IP pública, regla /32 del grupo de seguridad y que el servicio esté activo.\"}");
		}
	}
}
