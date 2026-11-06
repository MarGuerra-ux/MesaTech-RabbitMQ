package cl.duoc.mesatech.solicitudes.dto;

import cl.duoc.mesatech.solicitudes.model.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoSolicitud estado) {
}
