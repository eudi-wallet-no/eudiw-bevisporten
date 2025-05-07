package no.idporten.eudiw.issuer.config;

import no.idporten.eudiw.issuer.api.Endpoints;
import no.idporten.eudiw.issuer.openid4vci.CredentialConfigurations;
import no.idporten.eudiw.issuer.openid4vci.CredentialIssuerMetadata;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Configuration
public class CredentialIssuerServerConfiguration {

    @Bean
    public CredentialIssuerMetadata credentialIssuerMetadata(CredentialIssuerServerProperties properties) {
        return CredentialIssuerMetadata.builder()
                .credentialIssuer(properties.getCredentialIssuer())
                .credentialEndpoint(endpointURI(properties.getCredentialIssuer(), Endpoints.CREDENTIAL_ENDPOINT))
                .credentialConfigurations(CredentialConfigurations.builder().build())
                .build();
    }

    protected URI endpointURI(URI issuerUri, String path) {
        return UriComponentsBuilder.fromUri(issuerUri).path(path).build().toUri();
    }

}
