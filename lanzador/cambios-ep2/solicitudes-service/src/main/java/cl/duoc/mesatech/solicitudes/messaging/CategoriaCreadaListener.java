package cl.duoc.mesatech.solicitudes.messaging;

import cl.duoc.mesatech.solicitudes.service.CategoriaLocalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumidor de solicitudes.catalogo.queue (EC2 B).
 * Modo AUTO de Spring AMQP: el ack se envía SOLO cuando este método retorna normalmente,
 * es decir, después de que la categoría quedó guardada en RDS Solicitudes.
 * Si guardar falla, la excepción se propaga y el mensaje vuelve a la cola.
 * La cola NO se declara aquí: el usuario consumidor solo tiene permiso de lectura.
 */
@Component
public class CategoriaCreadaListener {

	private static final Logger log = LoggerFactory.getLogger(CategoriaCreadaListener.class);

	private final CategoriaLocalService service;
	private final JsonMapper jsonMapper;
	/** Demora artificial por mensaje (ms) para la demo: permite ver la cola crecer y vaciarse. 0 = sin demora. */
	private final long demoraMs;

	public CategoriaCreadaListener(CategoriaLocalService service, JsonMapper jsonMapper,
			@Value("${mesatech.consumer.delay-ms:0}") long demoraMs) {
		this.service = service;
		this.jsonMapper = jsonMapper;
		this.demoraMs = Math.max(0, demoraMs);
		log.info("[CONSUMIDOR] demora por mensaje = {} ms", this.demoraMs);
	}

	@RabbitListener(queues = "${mesatech.rabbitmq.queue}")
	public void recibir(Message mensaje) {
		String messageId = mensaje.getMessageProperties().getMessageId();
		boolean reentregado = Boolean.TRUE.equals(mensaje.getMessageProperties().getRedelivered());

		EventoCategoriaCreada evento;
		try {
			evento = jsonMapper.readValue(mensaje.getBody(), EventoCategoriaCreada.class);
		} catch (RuntimeException e) {
			// Un JSON inválido nunca podrá procesarse: se rechaza sin reencolar para no bloquear la cola
			log.error("[RECHAZADO] messageId={} JSON inválido: {}", messageId, e.getMessage());
			throw new AmqpRejectAndDontRequeueException("JSON inválido", e);
		}

		if (evento.categoriaId() == null || evento.eventId() == null || evento.nombre() == null) {
			log.error("[RECHAZADO] messageId={} faltan campos obligatorios: {}", messageId, evento);
			throw new AmqpRejectAndDontRequeueException("Evento incompleto");
		}

		log.info("[RECIBIDO] eventId={} messageId={} categoriaId={} nombre='{}' fechaEvento={} reentregado={}",
				evento.eventId(), messageId, evento.categoriaId(), evento.nombre(), evento.fechaEvento(), reentregado);

		if (demoraMs > 0) {
			// Demo: el mensaje queda Unacked mientras se "procesa"; el resto espera en Ready
			try {
				Thread.sleep(demoraMs);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Consumo interrumpido", e);
			}
		}

		// Guardar en RDS Solicitudes. Si lanza excepción -> nack + requeue (no se pierde el mensaje)
		service.guardarDesdeEvento(evento);

		log.info("[PERSISTIDO RDS SOLICITUDES] eventId={} categoriaId={} -> tabla categoria_local. Ack enviado al retornar.",
				evento.eventId(), evento.categoriaId());
	}
}
