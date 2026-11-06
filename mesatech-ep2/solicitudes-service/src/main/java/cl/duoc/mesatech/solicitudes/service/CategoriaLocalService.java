package cl.duoc.mesatech.solicitudes.service;

import cl.duoc.mesatech.solicitudes.messaging.EventoCategoriaCreada;
import cl.duoc.mesatech.solicitudes.model.CategoriaLocal;
import cl.duoc.mesatech.solicitudes.repository.CategoriaLocalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CategoriaLocalService {

	private static final Logger log = LoggerFactory.getLogger(CategoriaLocalService.class);

	private final CategoriaLocalRepository repository;

	public CategoriaLocalService(CategoriaLocalRepository repository) {
		this.repository = repository;
	}

	public List<CategoriaLocal> listar() {
		return repository.findAll(Sort.by("categoriaId"));
	}

	/**
	 * Guarda la copia local usando el categoriaId de origen como clave.
	 * Si el mismo evento se entrega dos veces, se actualiza la misma fila (no se duplica).
	 */
	@Transactional
	public CategoriaLocal guardarDesdeEvento(EventoCategoriaCreada evento) {
		CategoriaLocal copia = repository.findById(evento.categoriaId()).orElse(null);
		if (copia != null) {
			log.warn("[YA EXISTIA] categoriaId={} eventoAnterior={} eventoNuevo={} (posible reentrega)",
					evento.categoriaId(), copia.getEventId(), evento.eventId());
		} else {
			copia = new CategoriaLocal();
			copia.setCategoriaId(evento.categoriaId());
		}
		copia.setNombre(evento.nombre());
		copia.setEventId(evento.eventId());
		copia.setFechaEvento(evento.fechaEvento());
		copia.setFechaRecepcion(LocalDateTime.now());
		return repository.save(copia);
	}
}
