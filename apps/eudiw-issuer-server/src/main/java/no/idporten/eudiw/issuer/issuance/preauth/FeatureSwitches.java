package no.idporten.eudiw.issuer.issuance.preauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server.features")
public class FeatureSwitches {

    private boolean preAuthTxCodeEnabled;

    public boolean isPreAuthTxCodeEnabled() {
        return preAuthTxCodeEnabled;
    }

    public void setPreAuthTxCodeEnabled(boolean preAuthTxCodeEnabled) {
        this.preAuthTxCodeEnabled = preAuthTxCodeEnabled;
    }
}
