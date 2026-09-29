package no.idporten.eudiw.verifier.trustlist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.idporten.eudiw.verifier.VerificationException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties("trustlists")
public record TrustlistsProperties(
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("3s") Duration connectTimeout,
        Map<@NotBlank String, @NotNull TrustlistEntry> namedTrustlists,
        List<@NotBlank String> pid,
        List<@NotBlank String> attestations
) {

    public TrustlistsProperties {
        namedTrustlists = namedTrustlists == null ? Map.of() : Map.copyOf(namedTrustlists);
        pid = pid == null ? List.of() : List.copyOf(pid);
        attestations = attestations == null ? List.of() : List.copyOf(attestations);
    }

    /**
     * Trustlists for PID credentials, in configured priority order. These are always ETSI TS 119 602
     * lists, delivered either as XML or as JSON in a JWS/JWT.
     */
    public List<TrustlistReference> getPidTrustlists() {
        return resolveTrustlists(pid, entry -> format602(entry.encoding()));
    }

    /**
     * Trustlists for non-PID attestations, in configured priority order. These are always
     * ETSI TS 119 612 lists, which are always XML.
     */
    public List<TrustlistReference> getAttestationTrustlists() {
        return resolveTrustlists(attestations, entry -> TrustlistFormat.ETSI_612_XML);
    }

    private static TrustlistFormat format602(TrustlistEncoding encoding) {
        return encoding == TrustlistEncoding.JSON ? TrustlistFormat.ETSI_602_JSON : TrustlistFormat.ETSI_602_XML;
    }

    private List<TrustlistReference> resolveTrustlists(List<String> names, java.util.function.Function<TrustlistEntry, TrustlistFormat> format) {
        return names.stream()
                .map(this::resolveTrustlist)
                .map(entry -> new TrustlistReference(entry.url(), format.apply(entry)))
                .toList();
    }

    private TrustlistEntry resolveTrustlist(String name) {
        TrustlistEntry entry = namedTrustlists.get(name);
        if (entry == null) {
            throw new VerificationException("invalid_trustlist", "Unknown trustlist: " + name);
        }
        return entry;
    }
}
