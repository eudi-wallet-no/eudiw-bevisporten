package no.idporten.eudiw.verifier.proxy.api.verification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import no.idporten.eudiw.verifier.proxy.openid4vp.dcql.DcqlQuery;

@Schema(title = "Start verification request", description = "Start credential verification request.  Use either credential_configuration_id or credential types vct or doctype", type = "object")
@JsonIgnoreProperties(ignoreUnknown = true)
public record StartVerificationRequest(

        @Schema(description= "dcql query.", example= "")
        @JsonProperty("dcql_query")
        DcqlQuery dcqlQuery,

        // TODO Delete properties below
        @Schema(description = "Credential issuer identifier.", example = "https://utsteder.test.eidas2sandkasse.net")
        @JsonProperty("credential_issuer")
        String credentialIssuer,

        @Schema(description = "Credential configuration id.", example = "no.digdir.eudiw.pid_mso_mdoc")
        @JsonProperty("credential_configuration_id")
        String credentialConfigurationId,

        @Schema(description = "Credential type (vct).", example = "urn:eudi:pid:1")
        @JsonProperty("vct")
        String vct,

        @Schema(description = "Credential type (doctype).", example = "eu.europa.ec.eudi.pid.1")
        @JsonProperty("doctype")
        String doctype

) {

}
