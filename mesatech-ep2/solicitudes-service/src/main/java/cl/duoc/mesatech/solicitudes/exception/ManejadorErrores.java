package cl.duoc.mesatech.solicitudes.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(NegocioException.class)
	public ResponseEntity<Map<String, Object>> negocio(NegocioException e) {
		return ResponseEntity.status(e.getStatus()).body(Map.of("error", e.getStatus().name(), "mensaje", e.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
		String detalle = e.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + ": " + f.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.badRequest().body(Map.of("error", "VALIDACION", "mensaje", detalle));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> cuerpoInvalido(HttpMessageNotReadableException e) {
		return ResponseEntity.badRequest().body(Map.of("error", "VALIDACION",
				"mensaje", "Cuerpo inválido. Estados permitidos: ABIERTA, EN_PROCESO, RESUELTA, CERRADA"));
	}
}
