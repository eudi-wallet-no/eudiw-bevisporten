package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@AllArgsConstructor
public class StartIssuanceResponse {

    @JsonProperty("credential_offer")
    private CredentialOffer credentialOffer;

}
