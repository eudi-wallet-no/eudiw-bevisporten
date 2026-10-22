package no.idporten.eudiw.verifier.api.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlQuery;

import java.net.URI;

@Schema(title = "Start verification request", description = "Start credential verification request.  Use client application id and define dcql query", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record StartVerificationRequest(

        @Schema(description= "dcql query.", example= ApiDocProperties.PID_MDOC_EXAMPLE)
        @JsonProperty("dcql_query")
        DcqlQuery dcqlQuery,

        @Schema(description = "Redirect uri used in response to wallet in same device flow", example = "https://app.eidas2sandkasse.dev/verification-ok")
        @JsonProperty("redirect_uri")
        URI redirectUri

) {

}
