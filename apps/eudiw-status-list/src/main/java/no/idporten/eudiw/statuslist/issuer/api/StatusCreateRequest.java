package no.idporten.eudiw.statuslist.issuer.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusCreateRequest(
        @Schema(description = "Antall statuser for oppretting på statuslista", example = "1") @JsonProperty("number_of_entries") @Min(1) int numberOfEntries) {
}
