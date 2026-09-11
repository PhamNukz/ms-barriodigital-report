package cl.duoc.barriodigital.report.domain;

import jakarta.persistence.*;
import java.time.Instant;

/** Copia liviana de cada evento de requests.events, para agregarla sin tocar la BD de requests. */
@Entity
@Table(name = "report_event")
public class ReportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 60)
    private String eventId;

    @Column(name = "tramite_id", nullable = false)
    private Long tramiteId;

    @Column(name = "tipo_id", nullable = false)
    private Long tipoId;

    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private String estadoNuevo;

    @Column(name = "event_timestamp", nullable = false)
    private Instant timestamp;

    protected ReportEvent() {
    }

    public ReportEvent(String eventId, Long tramiteId, Long tipoId, String estadoNuevo, Instant timestamp) {
        this.eventId = eventId;
        this.tramiteId = tramiteId;
        this.tipoId = tipoId;
        this.estadoNuevo = estadoNuevo;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public Long getTramiteId() { return tramiteId; }
    public Long getTipoId() { return tipoId; }
    public String getEstadoNuevo() { return estadoNuevo; }
    public Instant getTimestamp() { return timestamp; }
}
