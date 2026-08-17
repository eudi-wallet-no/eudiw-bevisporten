package no.idporten.eudiw.connector.authoritativesources.config;

import no.idporten.validators.identifier.PersonIdentifierValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PersonIdentifierValidationProperties.class)
public class FeatureSwitches implements InitializingBean {
    private final PersonIdentifierValidationProperties personIdentifierValidationProperties;
    private final Logger log = LoggerFactory.getLogger(FeatureSwitches.class);

    public FeatureSwitches(PersonIdentifierValidationProperties personIdentifierValidationProperties) {
        this.personIdentifierValidationProperties = personIdentifierValidationProperties;
    }

    @Override
    public void afterPropertiesSet() {
        PersonIdentifierValidator.setSyntheticPersonIdentifiersAllowed(personIdentifierValidationProperties.allowSyntheticPid());
        PersonIdentifierValidator.setRealPersonIdentifiersAllowed(personIdentifierValidationProperties.allowRealPid());
        log.info("Will {}accept synthetic person identifiers from eId.", personIdentifierValidationProperties.allowSyntheticPid() ? "" : "not ");
        log.info("Will {}accept real person identifiers from eId.", personIdentifierValidationProperties.allowRealPid() ? "" : "not ");
    }
}
