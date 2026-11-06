package cl.duoc.mesatech.catalogo.messaging;

import cl.duoc.mesatech.catalogo.exception.PublicacionException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publicador de eventos de Catálogo.
 * - Mensaje JSON persistente (delivery mode 2) para sobrevivir al reinicio del broker.
 * - Confirmación del broker (publisher confirm) correlacionada por eventId.
 * - Retorno (mandatory) si el exchange no tiene una cola enlazada para la routing key.
 * El exchange, la cola y el binding NO se declaran aquí: los crea rabbitmq/setup-mesatech.sh
 * y el usuario publicador no tiene permiso "configure".
 */
@Component
public class CategoriaEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(CategoriaEventPublisher.class);

	private final RabbitTemplate rabbitTemplate;
	private final JsonMapper jsonMapper;
	private final String exchange;
	private final String routingKey;
	private final long confirmTimeoutMs;

	public CategoriaEventPublisher(RabbitTemplate rabbitTemplate, JsonMapper jsonMapper,
			@Value("${mesatech.rabbitmq.exchange}") String exchange,
			@Value("${mesatech.rabbitmq.routing-key}") String routingKey,
			@Value("${mesatech.rabbitmq.confirm-timeout-ms:5000}") long confirmTimeoutMs) {
		this.rabbitTemplate = rabbitTemplate;
		this.jsonMapper = jsonMapper;
		this.exchange = exchange;
		this.routingKey = routingKey;
		this.confirmTimeoutMs = confirmTimeoutMs;
	}

	@PostConstruct
	void registrarCallbackRetornos() {
		rabbitTemplate.setReturnsCallback(returned -> log.warn(
				"[RETORNADO] Mensaje sin ruta. messageId={} exchange={} routingKey={} replyCode={} replyText={}",
				returned.getMessage().getMessageProperties().getMessageId(), returned.getExchange(),
				returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText()));
	}

	/**
	 * Publica el evento y espera la confirmación del broker.
	 * Lanza PublicacionException si no hay conexión, si el broker responde nack,
	 * si el mensaje es retornado por falta de ruta o si se agota el tiempo de espera.
	 */
	public void publicar(EventoCategoriaCreada evento) {
		byte[] cuerpo = jsonMapper.writeValueAsBytes(evento);

		Message mensaje = MessageBuilder.withBody(cuerpo)
				.setContentType(MessageProperties.CONTENT_TYPE_JSON)
				.setContentEncoding("UTF-8")
				.setMessageId(evento.eventId())
				.setType(evento.tipoEvento())
				.setDeliveryMode(MessageDeliveryMode.PERSISTENT)
				.setHeader("categoriaId", evento.categoriaId())
				.build();

		CorrelationData correlacion = new CorrelationData(evento.eventId());

		try {
			rabbitTemplate.send(exchange, routingKey, mensaje, correlacion);

			CorrelationData.Confirm confirmacion = correlacion.getFuture().get(confirmTimeoutMs, TimeUnit.MILLISECONDS);

			if (!confirmacion.isAck()) {
				throw new PublicacionException("El broker rechazó el mensaje (nack): " + confirmacion.getReason());
			}
			ReturnedMessage retornado = correlacion.getReturned();
			if (retornado != null) {
				throw new PublicacionException("Mensaje retornado sin ruta a la cola: " + retornado.getReplyText());
			}

			log.info("[PUBLICADO] categoriaId={} eventId={} exchange={} routingKey={} confirmacion=ACK payload={}",
					evento.categoriaId(), evento.eventId(), exchange, routingKey,
					new String(cuerpo, java.nio.charset.StandardCharsets.UTF_8));

		} catch (AmqpException e) {
			throw new PublicacionException("No fue posible conectar o publicar en RabbitMQ: " + e.getMessage(), e);
		} catch (TimeoutException e) {
			throw new PublicacionException("Sin confirmación del broker en " + confirmTimeoutMs + " ms", e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PublicacionException("Publicación interrumpida", e);
		} catch (ExecutionException e) {
			throw new PublicacionException("Error esperando la confirmación: " + e.getMessage(), e);
		}
	}
}
