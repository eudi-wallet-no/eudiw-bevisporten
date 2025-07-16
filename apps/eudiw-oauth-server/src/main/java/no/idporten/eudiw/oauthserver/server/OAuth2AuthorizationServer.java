package no.idporten.eudiw.oauthserver.server;

import no.idporten.sdk.oidcserver.OpenIDConnectIntegrationBase;
import no.idporten.sdk.oidcserver.config.OpenIDConnectSdkConfiguration;

public class OAuth2AuthorizationServer extends OpenIDConnectIntegrationBase {


    public OAuth2AuthorizationServer(OpenIDConnectSdkConfiguration sdkConfiguration) {
        super(sdkConfiguration);
    }

}
