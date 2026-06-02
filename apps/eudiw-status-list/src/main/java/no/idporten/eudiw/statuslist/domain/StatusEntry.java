package no.idporten.eudiw.statuslist.domain;

import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;

public record StatusEntry(
        @Schema(description = "Index på statuslist", example = "1")
        int idx,

        @Schema(description = "URI til statusliste der index ligg", example = "https://example.com/status/lists/1")
        URI uri
) {
}
