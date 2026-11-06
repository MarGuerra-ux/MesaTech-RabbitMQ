package cl.duoc.mesatech.catalogo.exception;

import org.springframework.http.HttpStatus;

/** Error de negocio con su código HTTP (404 no encontrado, 409 duplicado). */
public class RecursoException extends RuntimeException {

	private final HttpStatus status;

	public RecursoException(HttpStatus status, String mensaje) {
		super(mensaje);
		this.status = status;
	}

	public HttpStatus getStatus() { return status; }
}
