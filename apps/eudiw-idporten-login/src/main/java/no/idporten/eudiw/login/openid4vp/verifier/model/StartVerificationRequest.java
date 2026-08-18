package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record StartVerificationRequest(
        @JsonProperty("dcql_query")
        DcqlQuery dcqlQuery,
        @JsonProperty("redirect_uri")
        URI redirectUri
) {
}
