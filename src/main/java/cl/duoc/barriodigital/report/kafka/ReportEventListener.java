package cl.duoc.barriodigital.report.kafka;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import cl.duoc.barriodigital.report.repo.ReportEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);

    private final ReportEventRepository repo;

    public ReportEventListener(ReportEventRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = "requests.events", groupId = "report-svc",
            autoStartup = "${barriodigital.kafka.listener-auto-startup:true}")
    @Transactional
    public void onTramiteEvent(TramiteEventIn evento) {
        if (repo.findByEventId(evento.eventId()).isPresent()) {
            log.info("evento {} ya agregado, se ignora", evento.eventId());
            return;
        }
        repo.save(new ReportEvent(evento.eventId(), evento.tramiteId(), evento.tipoId(),
                evento.estadoNuevo(), evento.timestamp()));
    }
}
