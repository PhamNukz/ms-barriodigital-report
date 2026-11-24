package cl.duoc.barriodigital.report.config;

import cl.duoc.barriodigital.report.kafka.TramiteEventIn;
import cl.duoc.barriodigital.report.service.EventoInvalidoException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Que pasa cuando un mensaje falla. Sin esto, un mensaje que siempre falla (poison pill)
 * deja la particion bloqueada.
 * <ul>
 *   <li>Error transitorio (BD caida): 3 reintentos con backoff de 1 s, 2 s y 4 s.</li>
 *   <li>JSON ilegible (ErrorHandlingDeserializer, ver yml) o payload invalido
 *       ({@link EventoInvalidoException}): sin reintentos, nunca se arreglan solos.</li>
 *   <li>Despues, a {@code <topico>.DLT} en la misma particion, con headers de la causa y del
 *       grupo de origen, y el offset avanza.</li>
 * </ul>
 * Spring Boot conecta este bean a la factory de los {@code @KafkaListener}.
 */
@Configuration
public class KafkaErrorConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorConfig.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaProperties props) {
        // La DLT recibe lo que fallo tal cual llego: los bytes si el JSON no se pudo leer,
        // o el DTO ya leido si fallo el servicio. byte[] primero: tambien es un Object.
        Map<Class<?>, Serializer<?>> porTipo = new LinkedHashMap<>();
        porTipo.put(byte[].class, new ByteArraySerializer());
        porTipo.put(Object.class, new JsonSerializer<>().noTypeInfo());
        KafkaTemplate<String, Object> dlt = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                props.buildProducerProperties(null), new StringSerializer(), new DelegatingByTypeSerializer(porTipo, true)));
        return errorHandler(dlt);
    }

    static DefaultErrorHandler errorHandler(KafkaOperations<?, ?> dlt) {
        var publicador = new DeadLetterPublishingRecoverer(dlt);
        ConsumerAwareRecordRecoverer aDlt = (rec, consumer, ex) -> {
            log.error("A {}.DLT: {}-{}@{} eventId={} causa={}", rec.topic(), rec.topic(), rec.partition(),
                    rec.offset(), eventId(rec.value()), NestedExceptionUtils.getMostSpecificCause(ex).toString());
            publicador.accept(rec, consumer, ex);
        };
        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);

        var handler = new DefaultErrorHandler(aDlt, backOff);
        handler.addNotRetryableExceptions(EventoInvalidoException.class);
        handler.setRetryListeners((rec, ex, intento) -> log.warn("Intento {} fallido {}-{}@{} eventId={}: {}",
                intento, rec.topic(), rec.partition(), rec.offset(), eventId(rec.value()),
                NestedExceptionUtils.getMostSpecificCause(ex).getMessage()));
        return handler;
    }

    private static String eventId(Object valor) {
        if (valor instanceof TramiteEventIn e) return e.eventId();
        return null; // JSON ilegible o mensaje vacio: no hay eventId que leer
    }
}
