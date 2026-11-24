package cl.duoc.barriodigital.report.config;

import cl.duoc.barriodigital.report.kafka.TramiteEventIn;
import cl.duoc.barriodigital.report.service.EventoInvalidoException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.ListenerExecutionFailedException;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.SerializationUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Sin broker: que hace el error handler con un mensaje que falla. */
class KafkaErrorConfigTest {

    @SuppressWarnings("unchecked")
    private final KafkaOperations<Object, Object> dlt = mock(KafkaOperations.class);
    @SuppressWarnings("unchecked")
    private final Consumer<Object, Object> consumer = mock(Consumer.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private final DefaultErrorHandler handler = KafkaErrorConfig.errorHandler(dlt);
    private final ConsumerRecord<Object, Object> rec = new ConsumerRecord<>("requests.events", 2, 42L, "10", null);

    KafkaErrorConfigTest() {
        when(dlt.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(null));
        // El recoverer verifica que la DLT tenga la particion de origen (kafkaadmin la crea con 3).
        when(consumer.partitionsFor(eq("requests.events.DLT"), any(Duration.class))).thenReturn(List.of(
                new PartitionInfo("requests.events.DLT", 0, null, null, null),
                new PartitionInfo("requests.events.DLT", 1, null, null, null),
                new PartitionInfo("requests.events.DLT", 2, null, null, null)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void evento_invalido_va_directo_a_la_dlt_misma_particion_y_con_el_grupo() {
        // Asi llega desde el contenedor: la excepcion del servicio envuelta, con el grupo.
        var ex = new ListenerExecutionFailedException("listener", "report-svc", new EventoInvalidoException("falta eventId"));

        boolean recuperado = handler.handleOne(ex, rec, consumer, container);

        assertTrue(recuperado, "sin reintentos: se recupera al primer fallo");
        ArgumentCaptor<ProducerRecord<Object, Object>> enviado = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(dlt).send(enviado.capture());
        assertEquals("requests.events.DLT", enviado.getValue().topic());
        assertEquals(2, enviado.getValue().partition());
        var grupo = enviado.getValue().headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_CONSUMER_GROUP);
        assertEquals("report-svc", new String(grupo.value(), StandardCharsets.UTF_8));
    }

    /** Con la config del yml, la poison pill ya no explota en el poll: queda marcada para la DLT. */
    @Test
    void json_roto_no_explota_y_queda_marcado_para_la_dlt() throws IOException {
        var yml = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml")).get(0);
        var kafka = new Binder(ConfigurationPropertySources.from(yml)).bind("spring.kafka", KafkaProperties.class).get();
        try (var deserializer = new ErrorHandlingDeserializer<Object>()) {
            deserializer.configure(kafka.buildConsumerProperties(null), false);

            var headers = new RecordHeaders();
            assertNull(deserializer.deserialize("requests.events", headers, "{\"hola\":".getBytes(StandardCharsets.UTF_8)));
            assertNotNull(headers.lastHeader(SerializationUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER));

            Object bueno = deserializer.deserialize("requests.events", new RecordHeaders(),
                    "{\"eventId\":\"e1\",\"tramiteId\":10,\"campoNuevo\":1}".getBytes(StandardCharsets.UTF_8));
            assertEquals("e1", ((TramiteEventIn) bueno).eventId());
        }
    }

    @Test
    void error_transitorio_se_reintenta_antes_de_ir_a_la_dlt() {
        var ex = new ListenerExecutionFailedException("listener", "report-svc", new IllegalStateException("BD caida"));

        boolean recuperado = handler.handleOne(ex, rec, consumer, container);

        assertFalse(recuperado, "primer fallo: queda para reintento");
        verify(dlt, never()).send(any(ProducerRecord.class));
    }
}
