package no.idporten.eudiw.oauth2.server;

import no.idporten.eudiw.oauth2.server.proxy.OIDCProxyProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("When validating headless authorization profile constraints")
class HeadlessAuthorizationProfileGuardTest {

    @Mock
    private OIDCProxyProperties oidcProxyProperties;
    @Mock
    private Environment environment;

    @Test
    @DisplayName("then startup fails if headless mode is enabled and prod profile is active")
    void shouldFailWhenHeadlessEnabledInProd() {
        OIDCProxyProperties.HeadlessLoginProperties headlessLogin = new OIDCProxyProperties.HeadlessLoginProperties();
        headlessLogin.setEnabled(true);
        when(oidcProxyProperties.getHeadlessLogin()).thenReturn(headlessLogin);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        HeadlessAuthorizationProfileGuard guard = new HeadlessAuthorizationProfileGuard(oidcProxyProperties, environment);

        assertThrows(IllegalStateException.class, guard::afterPropertiesSet);
    }

    @Test
    @DisplayName("then startup continues if headless mode is enabled outside prod")
    void shouldPassWhenHeadlessEnabledOutsideProd() {
        OIDCProxyProperties.HeadlessLoginProperties headlessLogin = new OIDCProxyProperties.HeadlessLoginProperties();
        headlessLogin.setEnabled(true);
        when(oidcProxyProperties.getHeadlessLogin()).thenReturn(headlessLogin);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"systest"});
        when(environment.getProperty("spring.application.environment")).thenReturn("systest");

        HeadlessAuthorizationProfileGuard guard = new HeadlessAuthorizationProfileGuard(oidcProxyProperties, environment);

        assertDoesNotThrow(guard::afterPropertiesSet);
    }
}
