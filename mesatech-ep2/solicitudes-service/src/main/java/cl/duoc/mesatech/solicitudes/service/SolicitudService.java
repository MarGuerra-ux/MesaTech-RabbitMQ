package cl.duoc.mesatech.solicitudes.service;

import cl.duoc.mesatech.solicitudes.dto.SolicitudRequest;
import cl.duoc.mesatech.solicitudes.exception.NegocioException;
import cl.duoc.mesatech.solicitudes.model.CategoriaLocal;
import cl.duoc.mesatech.solicitudes.model.EstadoSolicitud;
import cl.duoc.mesatech.solicitudes.model.Solicitud;
import cl.duoc.mesatech.solicitudes.repository.CategoriaLocalRepository;
import cl.duoc.mesatech.solicitudes.repository.SolicitudRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SolicitudService {

	private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

	private final SolicitudRepository solicitudes;
	private final CategoriaLocalRepository categorias;

	public SolicitudService(SolicitudRepository solicitudes, CategoriaLocalRepository categorias) {
		this.solicitudes = solicitudes;
		this.categorias = categorias;
	}

	public List<Solicitud> listar() {
		return solicitudes.findAll(Sort.by(Sort.Direction.DESC, "id"));
	}

	/** Nombre de cada categoría local, para la respuesta v2. */
	public Map<Long, String> nombresCategorias() {
		return categorias.findAll().stream()
				.collect(Collectors.toMap(CategoriaLocal::getCategoriaId, CategoriaLocal::getNombre, (a, b) -> a));
	}

	public Solicitud crear(SolicitudRequest request, String solicitante) {
		// Solo se aceptan categorías que YA llegaron por RabbitMQ a la copia local
		if (!categorias.existsById(request.categoriaId())) {
			throw new NegocioException(HttpStatus.valueOf(422),
					"La categoría " + request.categoriaId() + " aún no está disponible en Solicitudes "
							+ "(el evento no ha sido consumido). Intente nuevamente en unos segundos.");
		}
		LocalDateTime ahora = LocalDateTime.now();
		Solicitud s = new Solicitud();
		s.setTitulo(request.titulo().trim());
		s.setDescripcion(request.descripcion().trim());
		s.setCategoriaId(request.categoriaId());
		s.setEstado(EstadoSolicitud.ABIERTA);
		s.setSolicitante(solicitante == null || solicitante.isBlank() ? "desconocido" : solicitante);
		s.setFechaCreacion(ahora);
		s.setFechaActualizacion(ahora);
		s = solicitudes.save(s);
		log.info("[SOLICITUD CREADA] id={} categoriaId={} solicitante={}", s.getId(), s.getCategoriaId(), s.getSolicitante());
		return s;
	}

	public Solicitud cambiarEstado(Long id, EstadoSolicitud destino) {
		Solicitud s = solicitudes.findById(id)
				.orElseThrow(() -> new NegocioException(HttpStatus.NOT_FOUND, "Solicitud " + id + " no existe"));
		if (!s.getEstado().puedeCambiarA(destino)) {
			String regla = destino == EstadoSolicitud.RESUELTA
					? "Solo se puede resolver una solicitud que esté EN_PROCESO."
					: "Transición no permitida.";
			throw new NegocioException(HttpStatus.CONFLICT,
					regla + " Estado actual: " + s.getEstado() + ", solicitado: " + destino);
		}
		s.setEstado(destino);
		s.setFechaActualizacion(LocalDateTime.now());
		return solicitudes.save(s);
	}
}
