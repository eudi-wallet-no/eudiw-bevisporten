package no.idporten.eudiw.login.openid4vp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "eudiw-idporten-login.openid4vp")
public class OpenID4VPProperties {

    String siop2ClientId;
    String baseUri;
    CredentialConfig credentialConfig = new CredentialConfig();

}
