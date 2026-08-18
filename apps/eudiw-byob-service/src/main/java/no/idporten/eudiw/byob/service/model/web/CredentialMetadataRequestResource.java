package no.idporten.eudiw.byob.service.model.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import no.idporten.eudiw.byob.service.model.Claims;
import no.idporten.eudiw.byob.service.model.CredentialMetadata;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CredentialMetadataRequestResource(
        @Valid
        @JsonProperty("display")
        @NotEmpty(message = "CredentialMetadata display is null or empty")
        List<DisplayRequestResource> display,

        @Valid
        @NotEmpty(message = "CredentialMetadata claims is null or empty")
        @JsonProperty("claims")
        List<ClaimsRequestResource> claims
) {
    public CredentialMetadata toCredentialMetadata() {
        List<no.idporten.eudiw.byob.service.model.Display> displays = this.display == null ? Collections.emptyList() : this.display.stream().map(DisplayRequestResource::toDisplay).toList();

        List<Claims> claims = this.claims == null ? Collections.emptyList() : this.claims.stream().map(ClaimsRequestResource::toClaims).toList();

        return new CredentialMetadata(
                displays,
                claims
        );
    }
}
