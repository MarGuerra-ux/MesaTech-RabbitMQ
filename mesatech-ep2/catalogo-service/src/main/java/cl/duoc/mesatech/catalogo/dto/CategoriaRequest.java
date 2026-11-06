package cl.duoc.mesatech.catalogo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 100, message = "El nombre admite hasta 100 caracteres")
		String nombre,

		@Size(max = 255, message = "La descripción admite hasta 255 caracteres")
		String descripcion) {
}
