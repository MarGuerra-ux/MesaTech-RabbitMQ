package cl.duoc.mesatech.solicitudes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Solicitud de soporte. Propietario: Solicitudes (RDS Solicitudes). */
@Entity
@Table(name = "solicitud")
public class Solicitud {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 150)
	private String titulo;

	@Column(nullable = false, length = 1000)
	private String descripcion;

	/** Referencia a categoria_local.categoria_id (copia local, nunca al RDS de Catálogo). */
	@Column(name = "categoria_id", nullable = false)
	private Long categoriaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoSolicitud estado;

	@Column(nullable = false, length = 150)
	private String solicitante;

	@Column(name = "fecha_creacion", nullable = false)
	private LocalDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private LocalDateTime fechaActualizacion;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getTitulo() { return titulo; }
	public void setTitulo(String titulo) { this.titulo = titulo; }
	public String getDescripcion() { return descripcion; }
	public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
	public Long getCategoriaId() { return categoriaId; }
	public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
	public EstadoSolicitud getEstado() { return estado; }
	public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
	public String getSolicitante() { return solicitante; }
	public void setSolicitante(String solicitante) { this.solicitante = solicitante; }
	public LocalDateTime getFechaCreacion() { return fechaCreacion; }
	public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
	public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
	public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }
}
