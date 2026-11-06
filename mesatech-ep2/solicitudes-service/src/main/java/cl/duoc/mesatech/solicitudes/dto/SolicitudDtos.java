package cl.duoc.mesatech.solicitudes.dto;

import cl.duoc.mesatech.solicitudes.model.CategoriaLocal;
import cl.duoc.mesatech.solicitudes.model.Solicitud;

/** Representaciones de la API: v1 (resumida) y v2 (extendida). Ambas coexisten. */
public final class SolicitudDtos {

	private SolicitudDtos() {
	}

	public record SolicitudV1(Long id, String titulo, String estado, Long categoriaId) {
		public static SolicitudV1 de(Solicitud s) {
			return new SolicitudV1(s.getId(), s.getTitulo(), s.getEstado().name(), s.getCategoriaId());
		}
	}

	public record SolicitudV2(Long id, String titulo, String descripcion, String estado,
			Long categoriaId, String categoriaNombre, String solicitante,
			String fechaCreacion, String fechaActualizacion) {
		public static SolicitudV2 de(Solicitud s, String categoriaNombre) {
			return new SolicitudV2(s.getId(), s.getTitulo(), s.getDescripcion(), s.getEstado().name(),
					s.getCategoriaId(), categoriaNombre, s.getSolicitante(),
					s.getFechaCreacion().toString(), s.getFechaActualizacion().toString());
		}
	}

	public record CategoriaLocalResponse(Long categoriaId, String nombre, String eventId,
			String fechaEvento, String fechaRecepcion) {
		public static CategoriaLocalResponse de(CategoriaLocal c) {
			return new CategoriaLocalResponse(c.getCategoriaId(), c.getNombre(), c.getEventId(),
					c.getFechaEvento(), c.getFechaRecepcion().toString());
		}
	}
}
