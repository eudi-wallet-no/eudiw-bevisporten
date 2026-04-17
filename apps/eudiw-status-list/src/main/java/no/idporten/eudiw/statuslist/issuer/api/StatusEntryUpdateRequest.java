package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

import java.net.URI;

public record StatusEntryUpdateRequest(
        @Schema(description = "Index på statuslist", example = "1") @PositiveOrZero int idx,
        @Schema(description = "URI til statusliste der index ligg", example = "https://example.com/status/lists/1") @NotNull URI uri,
        @Schema(description = "Status type values. Verdi for revokasjon er 'INVALID' og einaste gyldig verdi", example = "INVALID") @JsonProperty("status_type") @Pattern(regexp = "^INVALID$", message = "Status_type må vera lik INVALID") String statusType) {
}
