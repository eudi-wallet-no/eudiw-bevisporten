package no.idporten.eudiw.verifier.statuslist;

import net.eidas2sandkasse.tsl.TokenStatusListValidator;
import no.idporten.eudiw.verifier.config.VerifierServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TokenStatusListValidatorConfiguration {

    @Bean
    public TokenStatusListValidator tokenStatusListValidator(VerifierServiceProperties verifierServiceProperties) {
        return new TokenStatusListValidator(verifierServiceProperties.getTokenStatusListValidatorConfig());
    }

}
