package no.idporten.eudiw.statuslist.logging.audit;

import java.util.List;

public record AuditLogEntries<T>(List<T> entries, int count) {
}