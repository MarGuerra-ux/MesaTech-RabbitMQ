package cl.duoc.mesatech.bff.controller;

import cl.duoc.mesatech.bff.client.MicroservicioClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Rutas de Solicitudes expuestas por API Gateway -> BFF. Coexisten v1 y v2. */
@RestController
public class SolicitudesBffController {

	private final MicroservicioClient client;

	public SolicitudesBffController(MicroservicioClient client) {
		this.client = client;
	}

	/** Categorías disponibles en la copia local de Solicitudes (recibidas por RabbitMQ). */
	@GetMapping("/api/v1/solicitudes/categorias")
	public ResponseEntity<String> categoriasLocales(Authentication auth) {
		return client.solicitudes(HttpMethod.GET, "/categorias", null, auth.getName());
	}

	@GetMapping("/api/v1/solicitudes")
	public ResponseEntity<String> listarV1(Authentication auth) {
		return client.solicitudes(HttpMethod.GET, "/v1/solicitudes", null, auth.getName());
	}

	@GetMapping("/api/v2/solicitudes")
	public ResponseEntity<String> listarV2(Authentication auth) {
		return client.solicitudes(HttpMethod.GET, "/v2/solicitudes", null, auth.getName());
	}

	@PostMapping({"/api/v1/solicitudes", "/api/v2/solicitudes"})
	public ResponseEntity<String> crear(@RequestBody String cuerpo, Authentication auth) {
		return client.solicitudes(HttpMethod.POST, "/solicitudes", cuerpo, auth.getName());
	}

	/** Cambiar estado: solo Admin. La regla EN_PROCESO -> RESUELTA la aplica Solicitudes. */
	@PreAuthorize("hasRole('Admin')")
	@PatchMapping({"/api/v1/solicitudes/{id}/estado", "/api/v2/solicitudes/{id}/estado"})
	public ResponseEntity<String> cambiarEstado(@PathVariable Long id, @RequestBody String cuerpo, Authentication auth) {
		return client.solicitudes(HttpMethod.PATCH, "/solicitudes/" + id + "/estado", cuerpo, auth.getName());
	}
}
