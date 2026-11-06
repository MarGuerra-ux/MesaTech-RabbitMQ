package cl.duoc.mesatech.catalogo.exception;

/** La categoría se guardó, pero RabbitMQ no confirmó el evento. */
public class PublicacionException extends RuntimeException {

	private Long categoriaId;
	private String eventId;

	public PublicacionException(String mensaje) {
		super(mensaje);
	}

	public PublicacionException(String mensaje, Throwable causa) {
		super(mensaje, causa);
	}

	public PublicacionException conDatos(Long categoriaId, String eventId) {
		this.categoriaId = categoriaId;
		this.eventId = eventId;
		return this;
	}

	public Long getCategoriaId() { return categoriaId; }
	public String getEventId() { return eventId; }
}
