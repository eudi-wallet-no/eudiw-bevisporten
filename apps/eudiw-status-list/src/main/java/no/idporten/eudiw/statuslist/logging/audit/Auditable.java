package no.idporten.eudiw.statuslist.logging.audit;

import java.util.Map;

public interface Auditable {
    Map<String, Object> toAudit();
}
