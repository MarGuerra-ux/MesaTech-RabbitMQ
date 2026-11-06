package cl.duoc.mesatech.solicitudes.controller;

import cl.duoc.mesatech.solicitudes.dto.CambioEstadoRequest;
import cl.duoc.mesatech.solicitudes.dto.SolicitudDtos.CategoriaLocalResponse;
import cl.duoc.mesatech.solicitudes.dto.SolicitudDtos.SolicitudV1;
import cl.duoc.mesatech.solicitudes.dto.SolicitudDtos.SolicitudV2;
import cl.duoc.mesatech.solicitudes.dto.SolicitudRequest;
import cl.duoc.mesatech.solicitudes.model.Solicitud;
import cl.duoc.mesatech.solicitudes.service.CategoriaLocalService;
import cl.duoc.mesatech.solicitudes.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API interna de Solicitudes (puerto 8082). Solo la consume el BFF (security group).
 * El usuario autenticado llega desde el BFF en la cabecera X-Usuario.
 */
@RestController
public class SolicitudesController {

	private final SolicitudService solicitudes;
	private final CategoriaLocalService categorias;

	public SolicitudesController(SolicitudService solicitudes, CategoriaLocalService categorias) {
		this.solicitudes = solicitudes;
		this.categorias = categorias;
	}

	/** Copia local de categorías recibidas por RabbitMQ (desde RDS Solicitudes). */
	@GetMapping("/categorias")
	public List<CategoriaLocalResponse> categoriasLocales() {
		return categorias.listar().stream().map(CategoriaLocalResponse::de).toList();
	}

	@GetMapping("/v1/solicitudes")
	public List<SolicitudV1> listarV1() {
		return solicitudes.listar().stream().map(SolicitudV1::de).toList();
	}

	@GetMapping("/v2/solicitudes")
	public List<SolicitudV2> listarV2() {
		Map<Long, String> nombres = solicitudes.nombresCategorias();
		return solicitudes.listar().stream().map(s -> SolicitudV2.de(s, nombres.get(s.getCategoriaId()))).toList();
	}

	@PostMapping("/solicitudes")
	public ResponseEntity<SolicitudV2> crear(@Valid @RequestBody SolicitudRequest request,
			@RequestHeader(value = "X-Usuario", required = false) String usuario) {
		Solicitud s = solicitudes.crear(request, usuario);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(SolicitudV2.de(s, solicitudes.nombresCategorias().get(s.getCategoriaId())));
	}

	@PatchMapping("/solicitudes/{id}/estado")
	public SolicitudV2 cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest request) {
		Solicitud s = solicitudes.cambiarEstado(id, request.estado());
		return SolicitudV2.de(s, solicitudes.nombresCategorias().get(s.getCategoriaId()));
	}
}
