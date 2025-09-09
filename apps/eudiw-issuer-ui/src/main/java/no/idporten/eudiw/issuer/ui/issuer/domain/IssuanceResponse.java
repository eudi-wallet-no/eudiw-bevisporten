package no.idporten.eudiw.issuer.ui.issuer.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuanceResponse(@JsonProperty("credential_offer") CredentialOffer credentialOffer, @JsonProperty("issuer_transaction_id") String issuerTransactionId) {
}
