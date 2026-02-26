package no.idporten.eudiw.issuer.config;

import java.io.IOException;
import java.net.URI;
import java.util.List;

public interface CredentialConfigurationSource {

    List<ExtendedCredentialConfiguration> retrieve(URI uri) throws IOException;

}
