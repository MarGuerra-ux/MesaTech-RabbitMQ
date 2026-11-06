package cl.duoc.mesatech.solicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
		@NotBlank(message = "El título es obligatorio") @Size(max = 150) String titulo,
		@NotBlank(message = "La descripción es obligatoria") @Size(max = 1000) String descripcion,
		@NotNull(message = "La categoría es obligatoria") Long categoriaId) {
}
