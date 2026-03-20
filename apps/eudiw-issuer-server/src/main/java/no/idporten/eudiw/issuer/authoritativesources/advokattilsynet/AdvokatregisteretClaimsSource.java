package no.idporten.eudiw.issuer.authoritativesources.advokattilsynet;

import lombok.SneakyThrows;
import no.idporten.eudiw.issuer.authoritativesources.AuthoritativeSource;
import no.idporten.eudiw.issuer.authoritativesources.advokattilsynet.model.PersonPrivate;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.CredentialData;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class AdvokatregisteretClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public static final String NAMESPACE = "no.advokattilsynet.advokatregisteret.1";

    private final AdvokatregisteretIntegration advokatregisteretIntegration;

    public AdvokatregisteretClaimsSource(AdvokatregisteretIntegration advokatregisteretIntegration) {
        this.advokatregisteretIntegration = advokatregisteretIntegration;

    }

    @SneakyThrows
    @Override
    public CredentialData pull(PreAuthorizedIssuanceContext preAuthorizedIssuanceContext) {
        String personIdentifier = preAuthorizedIssuanceContext.accessToken().getJWTClaimsSet().getStringClaim("pid");
        PersonPrivate personPrivate = advokatregisteretIntegration.retrieve(personIdentifier);
        Map<String, Object> claims = new HashMap<>();
        claims.put("personidentifikator", personIdentifier);
        claims.put("tittel", personPrivate.tittel());
        claims.put("etternavn", personPrivate.etternavn());
        claims.put("fornavn", personPrivate.fornavn());
        claims.put("mellomnavn", personPrivate.mellomnavn());
        claims.put("regnr", personPrivate.regnr());
        claims.values().removeIf(Objects::isNull);
        return new CredentialData(claims);
    }

    @Override
    public String getAuthorativeSourceName() {
        return AuthoritativeSource.ADVOKATREGISTERET.name();
    }

}
