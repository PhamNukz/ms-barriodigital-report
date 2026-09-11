package cl.duoc.barriodigital.report.kafka;

import java.time.Instant;

/** Misma forma que TramiteEvent en ms-barriodigital-requests (ver security para el porque duplicar). */
public record TramiteEventIn(
        String eventId,
        Instant timestamp,
        String traceId,
        Long tramiteId,
        Long tipoId,
        String vecinoUsername,
        String estadoAnterior,
        String estadoNuevo,
        String usuario) {
}
