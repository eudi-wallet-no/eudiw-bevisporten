package no.idporten.eudiw.issuer.openid4vci.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.HashMap;

@Builder
@AllArgsConstructor
public class CredentialConfigurations extends HashMap<String, CredentialConfiguration> {

}
