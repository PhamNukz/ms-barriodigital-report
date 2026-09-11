package cl.duoc.barriodigital.report.service;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import cl.duoc.barriodigital.report.repo.ReportEventRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KpiServiceTest {

    @Test
    void rango_invalido_lanza_excepcion() {
        var service = new KpiService(mock(ReportEventRepository.class));
        assertThat(catchIllegalArgument(() -> service.kpis("last30m"))).isTrue();
    }

    @Test
    void cuenta_tramites_activos_por_ultimo_estado_conocido() {
        ReportEventRepository repo = mock(ReportEventRepository.class);
        Instant ahora = Instant.now();
        when(repo.findAll()).thenReturn(List.of(
                new ReportEvent("e1", 1L, 10L, "INGRESADO", ahora.minus(2, ChronoUnit.HOURS)),
                new ReportEvent("e2", 1L, 10L, "ADMITIDO", ahora.minus(1, ChronoUnit.HOURS)), // tramite 1: activo
                new ReportEvent("e3", 2L, 10L, "INGRESADO", ahora.minus(3, ChronoUnit.HOURS)),
                new ReportEvent("e4", 2L, 10L, "RESUELTO", ahora.minus(30, ChronoUnit.MINUTES)) // tramite 2: terminal
        ));
        var service = new KpiService(repo);

        var kpis = service.kpis("last24h");

        assertThat(kpis.tramitesActivos()).isEqualTo(1);
        assertThat(kpis.tiempoResolucionPromedioMin()).isGreaterThan(0);
    }

    @Test
    void top_procedures_ordena_por_cantidad_descendente() {
        ReportEventRepository repo = mock(ReportEventRepository.class);
        Instant ahora = Instant.now();
        when(repo.findAll()).thenReturn(List.of(
                new ReportEvent("e1", 1L, 10L, "INGRESADO", ahora),
                new ReportEvent("e2", 2L, 10L, "INGRESADO", ahora),
                new ReportEvent("e3", 3L, 20L, "INGRESADO", ahora)
        ));
        var service = new KpiService(repo);

        var top = service.topProcedures("last7d");

        assertThat(top).hasSize(2);
        assertThat(top.get(0).tipoId()).isEqualTo(10L);
        assertThat(top.get(0).cantidad()).isEqualTo(2);
    }

    private boolean catchIllegalArgument(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }
}
