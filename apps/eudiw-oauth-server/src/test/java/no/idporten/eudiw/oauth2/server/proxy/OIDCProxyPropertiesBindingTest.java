package no.idporten.eudiw.oauth2.server.proxy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("When binding OIDC proxy properties")
class OIDCProxyPropertiesBindingTest {

    @Test
    @DisplayName("then headless-login config is bound to headlessLogin")
    void shouldBindHeadlessLoginConfiguration() {
        StandardEnvironment environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("test", Map.of(
                "oidc-proxy.headless-login.enabled", "true",
                "oidc-proxy.headless-login.synthetic_pid", "16903349844",
                "oidc-proxy.headless-login.acr", "idporten-loa-substantial",
                "oidc-proxy.headless-login.client-ids[0]", "wallet-client"
        )));

        OIDCProxyProperties properties = Binder.get(environment)
                .bind("oidc-proxy", Bindable.of(OIDCProxyProperties.class))
                .orElse(null);

        assertNotNull(properties);
        assertNotNull(properties.getHeadlessLogin());
        assertEquals(true, properties.getHeadlessLogin().isEnabled());
        assertEquals("16903349844", properties.getHeadlessLogin().getSyntheticPid());
        assertEquals("idporten-loa-substantial", properties.getHeadlessLogin().getAcr());
        assertEquals("wallet-client", properties.getHeadlessLogin().getClientIds().getFirst());
    }
}
