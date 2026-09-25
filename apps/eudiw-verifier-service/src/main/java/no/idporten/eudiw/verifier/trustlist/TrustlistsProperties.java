package no.idporten.eudiw.verifier.trustlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties("trustlists")
public record TrustlistsProperties(
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        Map<@NotBlank String, @NotNull URI> issuerTrustlists,
        Map<@NotBlank String, @NotNull URI> namedTrustlists,
        List<@NotBlank String> pid,
        List<@NotBlank String> attestations
) {

    private static final String DEFAULT_ISSUER = "default";

    public TrustlistsProperties {
        issuerTrustlists = issuerTrustlists == null ? Map.of() : Map.copyOf(issuerTrustlists);
        namedTrustlists = namedTrustlists == null ? Map.of() : Map.copyOf(namedTrustlists);
        pid = pid == null ? List.of() : List.copyOf(pid);
        attestations = attestations == null ? List.of() : List.copyOf(attestations);
    }

    public TrustlistReference getTrustlistForIssuer(String issuer) {
        URI trustlist = issuerTrustlists.get(issuer);
        if (trustlist != null) {
            return new TrustlistReference(trustlist, TrustlistFormat.ETSI_602);
        }
        trustlist = issuerTrustlists.get(DEFAULT_ISSUER);
        if (trustlist == null) {
            throw new VerificationException("invalid_trustlist", "Missing default issuer trustlist");
        }
        return new TrustlistReference(trustlist, TrustlistFormat.ETSI_612_XML);
    }

    public List<TrustlistReference> getPidTrustlists() {
        return resolveTrustlists(pid, TrustlistFormat.ETSI_602);
    }

    public List<TrustlistReference> getAttestationTrustlists() {
        return resolveTrustlists(attestations, TrustlistFormat.ETSI_612_XML);
    }

    private List<TrustlistReference> resolveTrustlists(List<String> names, TrustlistFormat format) {
        return names.stream().map(name -> new TrustlistReference(resolveTrustlist(name), format)).toList();
    }

    private URI resolveTrustlist(String name) {
        URI trustlist = namedTrustlists.get(name);
        if (trustlist == null) {
            throw new VerificationException("invalid_trustlist", "Unknown trustlist: " + name);
        }
        return trustlist;
    }
}
