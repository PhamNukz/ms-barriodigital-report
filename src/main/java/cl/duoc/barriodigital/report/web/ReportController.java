package cl.duoc.barriodigital.report.web;

import cl.duoc.barriodigital.report.service.KpiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Solo lectura, solo Admin -- KPIs comunales de la seccion 6 del caso. */
@RestController
@RequestMapping("/report")
@PreAuthorize("hasRole('Admin')")
public class ReportController {

    private final KpiService kpis;

    public ReportController(KpiService kpis) {
        this.kpis = kpis;
    }

    @GetMapping("/kpis")
    public ReportDtos.KpisResponse kpis(@RequestParam(defaultValue = "last24h") String range) {
        return kpis.kpis(range);
    }

    @GetMapping("/top-procedures")
    public List<ReportDtos.TopProcedure> topProcedures(@RequestParam(defaultValue = "last7d") String range) {
        return kpis.topProcedures(range);
    }
}
