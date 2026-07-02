package no.idporten.eudiw.oauth2.server;

import lombok.RequiredArgsConstructor;
import no.idporten.eudiw.oauth2.server.proxy.OIDCProxyProperties;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Prevents accidental enabling of headless authorization mode in production.
 * Should not happen, since Idporten does not support headless authorization in production. But just in case.
 */
@Component
@RequiredArgsConstructor
public class HeadlessAuthorizationProfileGuard implements InitializingBean {

    private final OIDCProxyProperties oidcProxyProperties;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        if (!oidcProxyProperties.getHeadlessLogin().isEnabled()) {
            return;
        }
        boolean prodProfileActive = Arrays.stream(environment.getActiveProfiles()).anyMatch(profile -> "prod".equalsIgnoreCase(profile));
        boolean prodEnvironment = "prod".equalsIgnoreCase(environment.getProperty("spring.application.environment"));
        if (prodProfileActive || prodEnvironment) {
            throw new IllegalStateException("Headless authorization code flow must not be enabled in production");
        }
    }
}
