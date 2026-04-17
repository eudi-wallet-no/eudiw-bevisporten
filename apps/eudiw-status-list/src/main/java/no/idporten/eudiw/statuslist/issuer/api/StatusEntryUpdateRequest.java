package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StatusEntryUpdateRequest(@PositiveOrZero int idx, @NotNull String uri, @JsonProperty("status_type") String statusType) {
}
