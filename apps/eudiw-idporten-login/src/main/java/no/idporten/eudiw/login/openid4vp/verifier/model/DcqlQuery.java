package no.idporten.eudiw.login.openid4vp.verifier.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DcqlQuery(
        List<DcqlCredentialQuery> credentials,
        @JsonProperty("credential_sets")
        List<DcqlCredentialSetQuery> credentialSets
) {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static DcqlQuery parse(String json) {
            return OBJECT_MAPPER.readValue(json, DcqlQuery.class);
    }
}
