package cl.duoc.barriodigital.report.web;

import java.time.Instant;
import java.util.List;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record TramitesPorHora(Instant hora, long cantidad) {
    }

    public record KpisResponse(
            List<TramitesPorHora> tramitesPorHora,
            long tramitesActivos,
            double tiempoResolucionPromedioMin) {
    }

    public record TopProcedure(Long tipoId, long cantidad) {
    }
}
