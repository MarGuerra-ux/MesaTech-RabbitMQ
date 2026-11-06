package cl.duoc.mesatech.catalogo.controller;

import cl.duoc.mesatech.catalogo.model.Prioridad;
import cl.duoc.mesatech.catalogo.repository.PrioridadRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/prioridades")
public class PrioridadController {

	private final PrioridadRepository repository;

	public PrioridadController(PrioridadRepository repository) {
		this.repository = repository;
	}

	@GetMapping
	public List<Prioridad> listar() {
		return repository.findAll(Sort.by("nivel"));
	}
}
