package no.idporten.eudiw.issuer.claimssource.skatteetaten;


import java.util.List;
import java.util.Map;

public record DocumentMetadata (
        // locale -> text
        Map<String, String> displayNames,
        List<ClaimMetadata> claims
){

    ClaimMetadata findClaimMetadata(String name) {
        return claims().stream().filter(claim -> claim.name().equals(name)).findFirst().orElse(null);
    }

}
