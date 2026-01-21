package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.byob.service.model.Claims;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimsRequestResource(
        @NotEmpty(message = "Claims path is null or empty")
        @JsonProperty("path")
        String path,
        @JsonProperty("type")
        String type,
        @JsonProperty("mandatory")
        boolean mandatory,
        @Valid
        @NotEmpty(message = "Claims display is null or empty")
        @JsonProperty("display")
        List<DisplayRequestResource> display
) {

    public Claims toClaims() {
        return new Claims(
                this.path,
                this.type,
                this.mandatory,
                this.display == null ? Collections.emptyList() : this.display.stream().map(DisplayRequestResource::toDisplay).toList()
        );
    }
}
