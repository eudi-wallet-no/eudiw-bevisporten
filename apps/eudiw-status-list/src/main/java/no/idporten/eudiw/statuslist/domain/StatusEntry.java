package no.idporten.eudiw.statuslist.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.statuslist.logging.audit.Auditable;

import java.net.URI;
import java.util.Map;

public record StatusEntry(
        @Schema(description = "Index på statuslist", example = "1")
        int idx,

        @Schema(description = "URI til statusliste der index ligg", example = "https://example.com/status/lists/1")
        URI uri
) implements Auditable {

    @Override
    public Map<String, Object> toAudit() {
        return Map.of("uri", uri().toString(), "idx", idx);
    }
}
