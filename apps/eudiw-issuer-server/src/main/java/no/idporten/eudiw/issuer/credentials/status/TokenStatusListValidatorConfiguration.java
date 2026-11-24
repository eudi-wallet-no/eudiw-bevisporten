package no.idporten.eudiw.issuer.credentials.status;

import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import no.idporten.eudiw.issuer.config.CredentialIssuerServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenStatusListValidatorConfiguration {

    @Bean
    public TokenStatusListValidator tokenStatusListValidator(CredentialIssuerServerProperties credentialIssuerServerProperties) {
        return new TokenStatusListValidator(credentialIssuerServerProperties.getTokenStatusListValidatorConfig());
    }

}
