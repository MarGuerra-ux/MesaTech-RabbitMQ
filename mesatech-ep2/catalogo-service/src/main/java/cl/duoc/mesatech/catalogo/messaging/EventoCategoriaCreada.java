package cl.duoc.mesatech.catalogo.messaging;

/**
 * Contrato del evento publicado en catalogo.exchange con routing key categoria.creada.
 *
 * eventId      String  UUID v4 único del evento (también va como messageId AMQP)
 * tipoEvento   String  Siempre "CATEGORIA_CREADA"
 * fechaEvento  String  ISO-8601 en UTC, ej: 2026-11-05T23:10:42.123456Z
 * categoriaId  Long    ID original de la categoría en RDS Catálogo (referencia de origen)
 * nombre       String  Nombre de la categoría
 */
public record EventoCategoriaCreada(
		String eventId,
		String tipoEvento,
		String fechaEvento,
		Long categoriaId,
		String nombre) {

	public static final String TIPO = "CATEGORIA_CREADA";
}
