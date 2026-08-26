package no.idporten.eudiw.issuer.api.revoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "credential-issuer-server.features.revocation-result-endpoint")
public class RevocationResultEndpointFeature implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(RevocationResultEndpointFeature.class);

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void afterPropertiesSet() {
        log.info("Will set revocation-result-endpoint enabled to {}", isEnabled());
    }
}
