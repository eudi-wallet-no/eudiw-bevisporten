package no.idporten.eudiw.issuer.claimssource.domain;


import java.util.Arrays;
import java.util.List;

public record DocumentMetadata(
    List<Display> displays,
    List<ClaimMetadata> claims
) {

    public ClaimMetadata findClaimMetadata(String name) {
        return claims().stream().filter(claim -> claim.name().equals(name)).findFirst().orElse(null);
    }

    public ClaimMetadata findClaimMetadata(String... path) {
        return claims().stream().filter(claim -> claim.path().equals(Arrays.stream(path).toList())).findFirst().orElse(null);
    }

    public record Display(
        String locale,
        String name,
        String description,
        String backgroundColor,
        String textColor
    ) {
        public Display(String locale, String name) {
            this(locale, name, null);
        }

        public Display(String locale, String name, String description) {
            this(locale, name, description, "#afcee9", "#002c54");
        }

        public Display(String locale, String name, String backgroundColor, String textColor) {
            this(locale, name, null, backgroundColor, textColor);
        }

    }
}
