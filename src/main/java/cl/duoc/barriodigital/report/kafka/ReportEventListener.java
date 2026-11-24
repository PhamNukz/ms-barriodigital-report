package cl.duoc.barriodigital.report.kafka;

import cl.duoc.barriodigital.report.service.ReportIngestService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Solo mensajeria: recibe, deja en el log de donde vino y delega en {@link ReportIngestService}.
 * Grupo report-svc (spring.kafka.consumer.group-id), con 3 hilos (listener.concurrency).
 * Los errores no se capturan aqui: los maneja KafkaErrorConfig (reintentos + DLT).
 */
@Component
public class ReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);

    private final ReportIngestService service;

    public ReportEventListener(ReportIngestService service) {
        this.service = service;
    }

    @KafkaListener(topics = "${barriodigital.kafka.topics.requests-events}",
            autoStartup = "${barriodigital.kafka.listener-auto-startup:true}")
    public void onTramiteEvent(ConsumerRecord<String, TramiteEventIn> rec) {
        log.info("Recibido {}-{}@{} key={}", rec.topic(), rec.partition(), rec.offset(), rec.key());
        service.registrar(rec.value());
    }
}
