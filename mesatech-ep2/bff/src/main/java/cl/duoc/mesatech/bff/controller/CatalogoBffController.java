package cl.duoc.mesatech.bff.controller;

import cl.duoc.mesatech.bff.client.MicroservicioClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rutas de Catálogo expuestas por API Gateway -> BFF (v1). */
@RestController
@RequestMapping("/api/v1/catalogo")
public class CatalogoBffController {

	private final MicroservicioClient client;

	public CatalogoBffController(MicroservicioClient client) {
		this.client = client;
	}

	@GetMapping("/categorias")
	public ResponseEntity<String> listar(Authentication auth) {
		return client.catalogo(HttpMethod.GET, "/categorias", null, auth.getName());
	}

	@GetMapping("/prioridades")
	public ResponseEntity<String> prioridades(Authentication auth) {
		return client.catalogo(HttpMethod.GET, "/prioridades", null, auth.getName());
	}

	/** Solo administradores: guarda en RDS Catálogo y publica categoria.creada. */
	@PreAuthorize("hasRole('Admin')")
	@PostMapping("/categorias")
	public ResponseEntity<String> crear(@RequestBody String cuerpo, Authentication auth) {
		return client.catalogo(HttpMethod.POST, "/categorias", cuerpo, auth.getName());
	}

	@PreAuthorize("hasRole('Admin')")
	@PutMapping("/categorias/{id}")
	public ResponseEntity<String> actualizar(@PathVariable Long id, @RequestBody String cuerpo, Authentication auth) {
		return client.catalogo(HttpMethod.PUT, "/categorias/" + id, cuerpo, auth.getName());
	}

	@PreAuthorize("hasRole('Admin')")
	@DeleteMapping("/categorias/{id}")
	public ResponseEntity<String> desactivar(@PathVariable Long id, Authentication auth) {
		return client.catalogo(HttpMethod.DELETE, "/categorias/" + id, null, auth.getName());
	}
}
