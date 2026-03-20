package no.idporten.eudiw.issuer.authoritativesources.krr;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.authoritativesources.krr.model.PersonKrr;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class KrrClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final KrrIntegration krrIntegration;

    public KrrClaimsSource(KrrIntegration krrIntegration) {
        this.krrIntegration = krrIntegration;
    }

    @SneakyThrows
    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext issuanceContext) {
        String personIdentifier = issuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid");
        PersonKrr personPrivate = krrIntegration.retrieve(personIdentifier);
        Map<String, Object> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("epostadresse", personPrivate.kontaktinformasjon().epostadresse());
        claims.put("mobiltelefonnummer", personPrivate.kontaktinformasjon().mobiltelefonnummer());
        claims.values().removeIf(Objects::isNull);
        return new CredentialData(claims);
    }

    @Override
    public String getAuthorativeSourceName(){
        return AuthoritativeSource.KRR.name();
    }
}
