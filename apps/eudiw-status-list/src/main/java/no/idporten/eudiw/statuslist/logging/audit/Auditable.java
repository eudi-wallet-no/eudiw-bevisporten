package no.idporten.eudiw.statuslist.logging.audit;


import tools.jackson.databind.JsonNode;

public interface Auditable {
    JsonNode toAudit();
}
