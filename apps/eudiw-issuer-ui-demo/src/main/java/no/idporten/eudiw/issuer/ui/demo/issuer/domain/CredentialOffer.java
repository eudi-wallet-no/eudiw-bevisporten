package no.idporten.eudiw.issuer.ui.demo.issuer.domain;

import java.util.List;

public record CredentialOffer(String credentialIssuer, List<String> credentialConfigurationIds, List<String> grants) {

}
