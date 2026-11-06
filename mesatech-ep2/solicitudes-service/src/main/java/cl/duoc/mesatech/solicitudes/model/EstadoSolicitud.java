package cl.duoc.mesatech.solicitudes.model;

import java.util.Map;
import java.util.Set;

/**
 * Reglas de estados (EP1): ABIERTA -> EN_PROCESO -> RESUELTA -> CERRADA.
 * Una solicitud solo puede pasar a RESUELTA si está EN_PROCESO.
 */
public enum EstadoSolicitud {
	ABIERTA, EN_PROCESO, RESUELTA, CERRADA;

	private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES = Map.of(
			ABIERTA, Set.of(EN_PROCESO, CERRADA),
			EN_PROCESO, Set.of(RESUELTA),
			RESUELTA, Set.of(CERRADA),
			CERRADA, Set.of());

	public boolean puedeCambiarA(EstadoSolicitud destino) {
		return TRANSICIONES.get(this).contains(destino);
	}
}
