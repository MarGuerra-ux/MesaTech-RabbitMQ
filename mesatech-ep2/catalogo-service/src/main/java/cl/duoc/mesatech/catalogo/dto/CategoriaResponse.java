package cl.duoc.mesatech.catalogo.dto;

import cl.duoc.mesatech.catalogo.model.Categoria;

public record CategoriaResponse(
		Long id,
		String nombre,
		String descripcion,
		Boolean activa,
		String eventId,
		String estadoPublicacion,
		String fechaCreacion) {

	public static CategoriaResponse de(Categoria c) {
		return new CategoriaResponse(c.getId(), c.getNombre(), c.getDescripcion(), c.getActiva(),
				c.getEventId(), c.getEstadoPublicacion(),
				c.getFechaCreacion() == null ? null : c.getFechaCreacion().toString());
	}
}
