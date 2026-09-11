package cl.duoc.barriodigital.report.service;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import cl.duoc.barriodigital.report.repo.ReportEventRepository;
import cl.duoc.barriodigital.report.web.ReportDtos.KpisResponse;
import cl.duoc.barriodigital.report.web.ReportDtos.TopProcedure;
import cl.duoc.barriodigital.report.web.ReportDtos.TramitesPorHora;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Agregaciones en memoria sobre la copia local de requests.events.
 * ponytail: a la escala de un ramo esto alcanza; con volumen real esto pasa a
 * consultas SQL agregadas (GROUP BY en la BD) o a una tabla de KPIs
 * pre-calculada por el propio listener en vez de recalcular en cada GET.
 */
@Service
public class KpiService {

    private static final Set<String> TERMINALES = Set.of("RESUELTO", "RECHAZADO");

    private final ReportEventRepository repo;

    public KpiService(ReportEventRepository repo) {
        this.repo = repo;
    }

    public Duration parseRange(String range) {
        return switch (range == null ? "last24h" : range) {
            case "last24h" -> Duration.ofHours(24);
            case "last7d" -> Duration.ofDays(7);
            default -> throw new IllegalArgumentException("range invalido: " + range + " (usa last24h o last7d)");
        };
    }

    public KpisResponse kpis(String range) {
        Instant desde = Instant.now().minus(parseRange(range));
        List<ReportEvent> todos = repo.findAll();

        List<TramitesPorHora> porHora = todos.stream()
                .filter(e -> "INGRESADO".equals(e.getEstadoNuevo()) && !e.getTimestamp().isBefore(desde))
                .collect(Collectors.groupingBy(e -> e.getTimestamp().truncatedTo(ChronoUnit.HOURS), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new TramitesPorHora(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(TramitesPorHora::hora))
                .toList();

        Map<Long, ReportEvent> ultimoPorTramite = new HashMap<>();
        for (ReportEvent e : todos) {
            ultimoPorTramite.merge(e.getTramiteId(), e,
                    (a, b) -> a.getTimestamp().isAfter(b.getTimestamp()) ? a : b);
        }
        long activos = ultimoPorTramite.values().stream()
                .filter(e -> !TERMINALES.contains(e.getEstadoNuevo()))
                .count();

        double promedioResolucionMin = tiempoResolucionPromedioMin(todos, desde);

        return new KpisResponse(porHora, activos, promedioResolucionMin);
    }

    private double tiempoResolucionPromedioMin(List<ReportEvent> todos, Instant desde) {
        Map<Long, List<ReportEvent>> porTramite = todos.stream()
                .collect(Collectors.groupingBy(ReportEvent::getTramiteId));

        List<Double> duracionesMin = new ArrayList<>();
        for (List<ReportEvent> eventos : porTramite.values()) {
            Optional<Instant> ingreso = eventos.stream()
                    .filter(e -> "INGRESADO".equals(e.getEstadoNuevo()))
                    .map(ReportEvent::getTimestamp).findFirst();
            Optional<Instant> resuelto = eventos.stream()
                    .filter(e -> "RESUELTO".equals(e.getEstadoNuevo()) && !e.getTimestamp().isBefore(desde))
                    .map(ReportEvent::getTimestamp).findFirst();
            if (ingreso.isPresent() && resuelto.isPresent()) {
                duracionesMin.add(Duration.between(ingreso.get(), resuelto.get()).toMinutes() * 1.0);
            }
        }
        return duracionesMin.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public List<TopProcedure> topProcedures(String range) {
        Instant desde = Instant.now().minus(parseRange(range));
        return repo.findAll().stream()
                .filter(e -> "INGRESADO".equals(e.getEstadoNuevo()) && !e.getTimestamp().isBefore(desde))
                .collect(Collectors.groupingBy(ReportEvent::getTipoId, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new TopProcedure(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(TopProcedure::cantidad).reversed())
                .toList();
    }
}
