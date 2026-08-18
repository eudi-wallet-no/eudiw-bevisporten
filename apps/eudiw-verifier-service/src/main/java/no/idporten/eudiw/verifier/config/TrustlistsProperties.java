package no.idporten.eudiw.verifier.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.Map;

@Validated
@ConfigurationProperties("trustlists")
public record TrustlistsProperties(
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        Map<@NotBlank String,@NotNull TrustlistContent> environments
) {

    public final static String PERSONAL = "eidas2sandkasse";
    public final static String WEBUILD = "webuild";

    private TrustlistContent getTrustlist(String key) {
        TrustlistContent trustlist = environments.get(key);
        if (trustlist == null) {
            throw new VerificationException("invalid_trustlist", "Invalid trustlist key: " + key);
        }
        return trustlist;
    }

    public TrustlistContent getWebuildTrustlist() {
        return getTrustlist(WEBUILD);
    }

    public TrustlistContent getPersonalTrustlist(){
        return getTrustlist(PERSONAL);
    }
}
