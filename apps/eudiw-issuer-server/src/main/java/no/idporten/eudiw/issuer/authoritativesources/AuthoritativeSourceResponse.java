package no.idporten.eudiw.issuer.authoritativesources;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
record AuthoritativeSourceResponse(
        @JsonProperty("credential_data") Map<String, Object> credentialData
) {
}
