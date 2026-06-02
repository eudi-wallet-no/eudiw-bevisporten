package no.idporten.eudiw.statuslist.logging.audit;

public interface Auditable<T> {
    T toAudit();
}
