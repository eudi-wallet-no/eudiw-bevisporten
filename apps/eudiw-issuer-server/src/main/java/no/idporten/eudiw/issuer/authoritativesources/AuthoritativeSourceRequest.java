package no.idporten.eudiw.issuer.authoritativesources;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.issuer.openid4vci.protocol.Subject;

record AuthoritativeSourceRequest(
        @JsonProperty("subject") Subject subject,
        @JsonProperty("credential_type") String credentialType
) {
}
