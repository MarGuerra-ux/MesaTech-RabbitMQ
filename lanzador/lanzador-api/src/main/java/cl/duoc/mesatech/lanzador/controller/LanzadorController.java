package cl.duoc.mesatech.lanzador.controller;

import cl.duoc.mesatech.lanzador.client.MesaTechClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api")
public class LanzadorController {

	private static final Logger log = LoggerFactory.getLogger(LanzadorController.class);
	private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HHmmss");

	private final MesaTechClient mesatech;
	private final AtomicInteger contador = new AtomicInteger();

	private final String rolRequerido;

	public LanzadorController(MesaTechClient mesatech, @Value("${lanzador.rol-requerido:Admin}") String rolRequerido) {
		this.mesatech = mesatech;
		this.rolRequerido = rolRequerido;
	}

	/** Datos del usuario autenticado según el token de Entra ID. */
	@GetMapping("/usuario")
	public Map<String, Object> usuario(@AuthenticationPrincipal Jwt jwt) {
		Map<String, Object> r = new LinkedHashMap<>();
		r.put("mensaje", "Token de Entra ID validado por el lanzador");
		r.put("nombre", jwt.getClaimAsString("name"));
		r.put("usuario", jwt.getClaimAsString("preferred_username"));
		r.put("scopes", jwt.getClaimAsString("scp"));
		r.put("roles", jwt.getClaimAsStringList("roles"));
		r.put("emisor", String.valueOf(jwt.getIssuer()));
		r.put("expira", String.valueOf(jwt.getExpiresAt()));
		return r;
	}

	/**
	 * Dispara UNA solicitud: crea una categoría en Catálogo (EC2 A), que la guarda en su RDS
	 * y publica categoria.creada en RabbitMQ. Nombre automático y único: Demo-HHmmss-0001.
	 * Cuerpo opcional: {"nombre":"..."} para usar un nombre propio.
	 */
	@PostMapping("/disparar")
	public ResponseEntity<String> disparar(@RequestBody(required = false) Map<String, String> cuerpo, Authentication auth) {
		// Autorización por App Role de Entra ID: sin el rol requerido -> 403 Forbidden
		boolean autorizado = auth.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals("ROLE_" + rolRequerido));
		if (!autorizado) {
			log.warn("[DISPARO RECHAZADO] usuario={} sin rol {} -> 403", auth.getName(), rolRequerido);
			throw new AccessDeniedException("Se requiere el rol " + rolRequerido + " para disparar solicitudes");
		}
		int n = contador.incrementAndGet();
		String nombre = (cuerpo != null && cuerpo.get("nombre") != null && !cuerpo.get("nombre").isBlank())
				? cuerpo.get("nombre").trim()
				: String.format("Demo-%s-%04d", LocalTime.now().format(HORA), n);
		String json = "{\"nombre\":\"" + nombre.replace("\"", "") + "\",\"descripcion\":\"Disparada desde el lanzador local por "
				+ auth.getName().replace("\"", "") + "\"}";
		long t0 = System.currentTimeMillis();
		ResponseEntity<String> r = mesatech.catalogo(HttpMethod.POST, "/categorias", json);
		log.info("[DISPARO #{}] usuario={} nombre={} -> HTTP {} en {} ms", n, auth.getName(), nombre,
				r.getStatusCode().value(), System.currentTimeMillis() - t0);
		return r;
	}

	/** Categorías en RDS Catálogo, con su estado de publicación. */
	@GetMapping("/catalogo/categorias")
	public ResponseEntity<String> categoriasCatalogo() {
		return mesatech.catalogo(HttpMethod.GET, "/categorias", null);
	}

	/** Copia local en RDS Solicitudes: categorías que el consumidor ya procesó. */
	@GetMapping("/solicitudes/categorias")
	public ResponseEntity<String> categoriasConsumidas() {
		return mesatech.solicitudes(HttpMethod.GET, "/categorias");
	}
}
