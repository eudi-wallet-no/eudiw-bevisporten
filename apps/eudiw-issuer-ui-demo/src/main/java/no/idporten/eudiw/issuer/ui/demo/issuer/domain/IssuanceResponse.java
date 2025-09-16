package no.idporten.eudiw.issuer.ui.demo.issuer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuanceResponse(@JsonProperty("credential_offer") CredentialOffer credentialOffer, @JsonProperty("issuance_transaction_id") String issuanceTransactionId) {
}
