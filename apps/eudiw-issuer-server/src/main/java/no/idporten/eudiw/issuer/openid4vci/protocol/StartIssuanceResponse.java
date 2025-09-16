package no.idporten.eudiw.issuer.openid4vci.protocol;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import no.idporten.eudiw.issuer.openid4vci.service.IssuanceTransactionId;

@Schema(description = "Start credential issue response", title = "Start credential issue response", type = "object")
@Builder
@AllArgsConstructor
public class StartIssuanceResponse {

    @Schema(description = "OpenID4VCI Credential offer", example = """
            {
                    "credential_issuer": "https://utsteder.test.eidas2sandkasse.net",
                    "credential_configuration_ids": [
                        "some.known.credential_mso_mdoc"
                    ],
                    "grants": {
                        "urn:ietf:params:oauth:grant-type:pre-authorized_code": {
                            "pre-authorized_code": "abc123",
                            "tx_code": {
                                "length": 4,
                                "input_mode": "numeric",
                                "description": "Enter code from SMS to issue Foobar credentials"
                            }
                        }
                    }
                }""")
    @JsonProperty("credential_offer")
    private CredentialOffer credentialOffer;

    @Schema(description = "Credential issuance transaction id", example = "xyz123...")
    @JsonProperty("issuance_transaction_id")
    private IssuanceTransactionId issuanceTransactionId;

}
