package cl.duoc.barriodigital.report.repo;

import cl.duoc.barriodigital.report.domain.ReportEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportEventRepository extends JpaRepository<ReportEvent, Long> {
    Optional<ReportEvent> findByEventId(String eventId);
    List<ReportEvent> findAllByTramiteIdOrderByTimestampAsc(Long tramiteId);
}
