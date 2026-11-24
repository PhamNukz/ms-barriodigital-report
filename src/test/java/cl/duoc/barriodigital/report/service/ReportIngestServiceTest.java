package cl.duoc.barriodigital.report.service;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import cl.duoc.barriodigital.report.kafka.TramiteEventIn;
import cl.duoc.barriodigital.report.repo.ReportEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReportIngestServiceTest {

    private final ReportEventRepository repo = mock(ReportEventRepository.class);
    private final ReportIngestService service = new ReportIngestService(repo);

    private static TramiteEventIn evento(String eventId, Long tipoId) {
        return new TramiteEventIn(eventId, Instant.parse("2030-03-01T10:00:00Z"), "trace-1", 10L, tipoId,
                "vecino1", "INGRESADO", "ADMITIDO", "func1");
    }

    @Test
    void guarda_un_evento_nuevo() {
        when(repo.findByEventId("evt-1")).thenReturn(Optional.empty());

        service.registrar(evento("evt-1", 3L));

        ArgumentCaptor<ReportEvent> guardado = ArgumentCaptor.forClass(ReportEvent.class);
        verify(repo).save(guardado.capture());
        assertEquals(10L, guardado.getValue().getTramiteId());
        assertEquals(3L, guardado.getValue().getTipoId());
        assertEquals("ADMITIDO", guardado.getValue().getEstadoNuevo());
    }

    @Test
    void ignora_una_reentrega() {
        when(repo.findByEventId("evt-dup")).thenReturn(Optional.of(
                new ReportEvent("evt-dup", 10L, 3L, "ADMITIDO", Instant.now())));

        service.registrar(evento("evt-dup", 3L));

        verify(repo, never()).save(any());
    }

    /** Validacion: no reintentable, va directo a la DLT (ver KafkaErrorConfigTest). */
    @Test
    void evento_sin_tipoId_es_invalido_y_no_se_guarda() {
        var ex = assertThrows(EventoInvalidoException.class, () -> service.registrar(evento("evt-2", null)));

        assertEquals("falta tipoId", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void mensaje_vacio_es_invalido() {
        assertThrows(EventoInvalidoException.class, () -> service.registrar(null));
    }
}
