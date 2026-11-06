package cl.duoc.mesatech.catalogo.controller;

import cl.duoc.mesatech.catalogo.dto.CategoriaRequest;
import cl.duoc.mesatech.catalogo.dto.CategoriaResponse;
import cl.duoc.mesatech.catalogo.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API interna de Catálogo (puerto 8081). Solo la consume el BFF por localhost en la EC2 A.
 */
@RestController
@RequestMapping("/categorias")
public class CategoriaController {

	private final CategoriaService service;

	public CategoriaController(CategoriaService service) {
		this.service = service;
	}

	@GetMapping
	public List<CategoriaResponse> listar() {
		return service.listarActivas().stream().map(CategoriaResponse::de).toList();
	}

	@GetMapping("/{id}")
	public CategoriaResponse obtener(@PathVariable Long id) {
		return CategoriaResponse.de(service.obtener(id));
	}

	@PostMapping
	public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(CategoriaResponse.de(service.crear(request)));
	}

	@PutMapping("/{id}")
	public CategoriaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
		return CategoriaResponse.de(service.actualizar(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> desactivar(@PathVariable Long id) {
		service.desactivar(id);
		return ResponseEntity.noContent().build();
	}
}
