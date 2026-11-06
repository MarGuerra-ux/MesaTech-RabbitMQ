package cl.duoc.mesatech.solicitudes.messaging;

/**
 * Contrato del evento recibido desde solicitudes.catalogo.queue (mismo contrato que publica Catálogo).
 * eventId String UUID · tipoEvento String "CATEGORIA_CREADA" · fechaEvento String ISO-8601 UTC
 * categoriaId Long (ID original en RDS Catálogo) · nombre String
 */
public record EventoCategoriaCreada(
		String eventId,
		String tipoEvento,
		String fechaEvento,
		Long categoriaId,
		String nombre) {
}
