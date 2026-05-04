package no.idporten.eudiw.statuslist.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.statuslist.logging.audit.Auditable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;

import static no.idporten.eudiw.statuslist.util.StatusListUtil.getListId;

public record StatusEntry(
        @Schema(description = "Index på statuslist", example = "1")
        int idx,

        @Schema(description = "URI til statusliste der index ligg", example = "https://example.com/status/lists/1")
        URI uri
) implements Auditable {

    @Override
    public JsonNode toAudit() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("list_id", getListId(uri()));
        node.put("idx", idx);
        return node;
    }
}
