package no.idporten.eudiw.issuer.ui.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// This class does not include config for issuer-server integration, only other configuration.
@Configuration
@ConfigurationProperties(ignoreInvalidFields = true, prefix = "bevisgenerator")
public class BevisgeneratorProperties {

    private final FeatureSwitches featureSwitches;

    @Autowired
    public BevisgeneratorProperties(FeatureSwitches featureSwitches) {
        this.featureSwitches = featureSwitches;
    }

    public FeatureSwitches getFeatureSwitches() {
        return featureSwitches;
    }

}
