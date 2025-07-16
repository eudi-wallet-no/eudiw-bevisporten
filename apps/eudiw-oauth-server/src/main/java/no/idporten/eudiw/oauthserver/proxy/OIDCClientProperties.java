package no.idporten.eudiw.oauthserver.proxy;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import no.idporten.eudiw.oauthserver.crypto.KeyProvider;
import no.idporten.eudiw.oauthserver.crypto.KeyStoreProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

@Data
@Validated
public class OIDCClientProperties {

    @NotNull
    private ClientID clientID;
    @NotNull
    private ClientAuthenticationMethod clientAuthenticationMethod;
    private Secret clientSecret;
    private KeyStoreProperties keystore;
    private KeyProvider keyProvider;

    public void validate() throws Exception {
        Objects.requireNonNull(clientID);
        Objects.requireNonNull(clientAuthenticationMethod);
        if (ClientAuthenticationMethod.PRIVATE_KEY_JWT.equals(clientAuthenticationMethod) && keystore == null) {
            throw new IllegalArgumentException("Keystore needed for private_key_jwt");
        }
        if (clientSecret == null && (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.equals(clientAuthenticationMethod) || ClientAuthenticationMethod.CLIENT_SECRET_POST.equals(clientAuthenticationMethod))) {
            throw new IllegalArgumentException("Client secret needed for %s".formatted(clientAuthenticationMethod.getValue()));
        }
    }

}
