package cl.duoc.mesatech.catalogo.service;

import cl.duoc.mesatech.catalogo.dto.CategoriaRequest;
import cl.duoc.mesatech.catalogo.exception.PublicacionException;
import cl.duoc.mesatech.catalogo.exception.RecursoException;
import cl.duoc.mesatech.catalogo.messaging.CategoriaEventPublisher;
import cl.duoc.mesatech.catalogo.messaging.EventoCategoriaCreada;
import cl.duoc.mesatech.catalogo.model.Categoria;
import cl.duoc.mesatech.catalogo.repository.CategoriaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Guardar en RDS y publicar son DOS operaciones separadas (sin transacción distribuida):
 *   1) se guarda la categoría (commit en RDS Catálogo)
 *   2) se publica el evento y se espera la confirmación del broker
 * Si 1 funciona y 2 falla, la categoría queda con estado_publicacion = ERROR_PUBLICACION
 * y se responde 503 de forma controlada (prueba P7).
 */
@Service
public class CategoriaService {

	private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);

	private final CategoriaRepository repository;
	private final CategoriaEventPublisher publisher;

	public CategoriaService(CategoriaRepository repository, CategoriaEventPublisher publisher) {
		this.repository = repository;
		this.publisher = publisher;
	}

	public List<Categoria> listarActivas() {
		return repository.findByActivaTrueOrderByIdAsc();
	}

	public Categoria obtener(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new RecursoException(HttpStatus.NOT_FOUND, "Categoría " + id + " no existe"));
	}

	public Categoria crear(CategoriaRequest request) {
		String nombre = request.nombre().trim();
		if (repository.existsByNombreIgnoreCase(nombre)) {
			throw new RecursoException(HttpStatus.CONFLICT, "Ya existe una categoría llamada '" + nombre + "'");
		}

		// 1) Persistir en RDS Catálogo
		Categoria categoria = new Categoria();
		categoria.setNombre(nombre);
		categoria.setDescripcion(request.descripcion());
		categoria.setActiva(true);
		categoria.setEventId(UUID.randomUUID().toString());
		categoria.setEstadoPublicacion("PENDIENTE");
		categoria.setFechaCreacion(LocalDateTime.now());
		categoria = repository.save(categoria);
		log.info("[GUARDADO RDS CATALOGO] categoriaId={} nombre='{}' eventId={}",
				categoria.getId(), categoria.getNombre(), categoria.getEventId());

		// 2) Publicar evento (operación separada)
		EventoCategoriaCreada evento = new EventoCategoriaCreada(
				categoria.getEventId(), EventoCategoriaCreada.TIPO, Instant.now().toString(),
				categoria.getId(), categoria.getNombre());
		try {
			publisher.publicar(evento);
			categoria.setEstadoPublicacion("PUBLICADA");
			return repository.save(categoria);
		} catch (PublicacionException e) {
			categoria.setEstadoPublicacion("ERROR_PUBLICACION");
			repository.save(categoria);
			log.error("[ERROR PUBLICACION] categoriaId={} eventId={} quedó guardada en RDS Catálogo. Causa: {}",
					categoria.getId(), categoria.getEventId(), e.getMessage());
			throw e.conDatos(categoria.getId(), categoria.getEventId());
		}
	}

	/** Actualización: mantiene el comportamiento de la EP1 (no propaga evento). */
	public Categoria actualizar(Long id, CategoriaRequest request) {
		Categoria categoria = obtener(id);
		categoria.setNombre(request.nombre().trim());
		categoria.setDescripcion(request.descripcion());
		return repository.save(categoria);
	}

	/** Baja lógica: mantiene el comportamiento de la EP1 (no propaga evento). */
	public void desactivar(Long id) {
		Categoria categoria = obtener(id);
		categoria.setActiva(false);
		repository.save(categoria);
	}
}
