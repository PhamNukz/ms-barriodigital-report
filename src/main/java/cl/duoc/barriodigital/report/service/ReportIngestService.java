package cl.duoc.barriodigital.report.service;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import cl.duoc.barriodigital.report.kafka.TramiteEventIn;
import cl.duoc.barriodigital.report.repo.ReportEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guarda la copia local de cada evento para los KPIs, sin nada de Kafka: valida, descarta
 * reentregas (at-least-once: el mismo eventId puede llegar dos veces) y guarda.
 */
@Service
public class ReportIngestService {

    private static final Logger log = LoggerFactory.getLogger(ReportIngestService.class);

    private final ReportEventRepository repo;

    public ReportIngestService(ReportEventRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void registrar(TramiteEventIn e) {
        exigir(e, "payload");
        exigir(e.eventId(), "eventId");
        exigir(e.tramiteId(), "tramiteId");
        exigir(e.tipoId(), "tipoId");
        exigir(e.estadoNuevo(), "estadoNuevo");
        exigir(e.timestamp(), "timestamp");
        if (repo.findByEventId(e.eventId()).isPresent()) {
            log.info("evento {} ya agregado, se ignora", e.eventId());
            return;
        }
        repo.save(new ReportEvent(e.eventId(), e.tramiteId(), e.tipoId(), e.estadoNuevo(), e.timestamp()));
    }

    /** Los campos que la tabla tiene NOT NULL: sin ellos el save fallaria igual, pero tras 3 reintentos inutiles. */
    private static void exigir(Object valor, String campo) {
        if (valor == null || valor instanceof String s && s.isBlank()) {
            throw new EventoInvalidoException("falta " + campo);
        }
    }
}
