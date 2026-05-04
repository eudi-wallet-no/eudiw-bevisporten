package no.idporten.eudiw.statuslist.logging.audit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import java.util.List;

public record AuditEntryCollection<T extends Auditable>(List<T> entries) implements Auditable {

    @Override
    public JsonNode toAudit() {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        entries.stream()
                .map(Auditable::toAudit)
                .forEach(array::add);
        return array;
    }
}
