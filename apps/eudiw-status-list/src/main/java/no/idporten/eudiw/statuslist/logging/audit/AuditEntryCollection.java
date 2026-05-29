package no.idporten.eudiw.statuslist.logging.audit;

import java.util.List;
import java.util.Map;

public record AuditEntryCollection<T extends Auditable>(List<T> entries) {

    public List<Map<String, Object>> toAudit() {
        return entries.stream()
                .map(Auditable::toAudit)
                .toList();
    }
}
