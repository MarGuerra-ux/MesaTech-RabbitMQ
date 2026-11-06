package cl.duoc.mesatech.catalogo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {

	/** P7: respuesta controlada. NO se afirma que se revirtió: la categoría quedó guardada. */
	@ExceptionHandler(PublicacionException.class)
	public ResponseEntity<Map<String, Object>> publicacion(PublicacionException e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("error", "EVENTO_NO_CONFIRMADO");
		body.put("mensaje", "La categoría fue guardada en RDS Catálogo, pero RabbitMQ no confirmó el evento. "
				+ "No se revirtió automáticamente: la copia en Solicitudes no se creará hasta reenviar el evento.");
		body.put("categoriaId", e.getCategoriaId());
		body.put("eventId", e.getEventId());
		body.put("estadoPublicacion", "ERROR_PUBLICACION");
		body.put("detalle", e.getMessage());
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
	}

	@ExceptionHandler(RecursoException.class)
	public ResponseEntity<Map<String, Object>> recurso(RecursoException e) {
		return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getStatus().name(), "mensaje", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
		String detalle = e.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + ": " + f.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest().body(Map.of("error", "VALIDACION", "mensaje", detalle));
	}
}
