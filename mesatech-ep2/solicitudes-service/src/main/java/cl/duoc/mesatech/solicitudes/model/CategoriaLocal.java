package cl.duoc.mesatech.solicitudes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Copia local de una categoría recibida por RabbitMQ. Propietario: Solicitudes (RDS Solicitudes).
 * La clave primaria es el categoriaId ORIGINAL de Catálogo: así se conserva la referencia de origen
 * y una reentrega del mismo evento actualiza la misma fila en vez de duplicarla.
 */
@Entity
@Table(name = "categoria_local")
public class CategoriaLocal {

	@Id
	@Column(name = "categoria_id")
	private Long categoriaId;

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(name = "event_id", nullable = false, length = 36)
	private String eventId;

	/** fechaEvento tal como viene en el mensaje (ISO-8601 UTC). */
	@Column(name = "fecha_evento", nullable = false, length = 40)
	private String fechaEvento;

	@Column(name = "fecha_recepcion", nullable = false)
	private LocalDateTime fechaRecepcion;

	public Long getCategoriaId() { return categoriaId; }
	public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
	public String getNombre() { return nombre; }
	public void setNombre(String nombre) { this.nombre = nombre; }
	public String getEventId() { return eventId; }
	public void setEventId(String eventId) { this.eventId = eventId; }
	public String getFechaEvento() { return fechaEvento; }
	public void setFechaEvento(String fechaEvento) { this.fechaEvento = fechaEvento; }
	public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
	public void setFechaRecepcion(LocalDateTime fechaRecepcion) { this.fechaRecepcion = fechaRecepcion; }
}
