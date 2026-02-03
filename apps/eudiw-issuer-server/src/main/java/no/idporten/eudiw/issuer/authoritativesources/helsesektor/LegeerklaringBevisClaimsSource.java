package no.idporten.eudiw.issuer.authoritativesources.helsesektor;

import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.credentials.types.ClaimDataType;
import no.idporten.eudiw.issuer.credentials.types.ClaimMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LegeerklaringBevisClaimsSource extends AbstractPreAuthorizedClaimsSource {

    public static final String NAMESPACE = "no:helsesektor:legeerklaring:1";

    private final DocumentMetadata documentMetadata;

    public LegeerklaringBevisClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Legeerklæringbevis", "Legeerklæringbevis utstedt av kommune (Hackathon Brukerrådet 2025)")),
                List.of(
                        new ClaimMetadata(NAMESPACE,
                                "fodselsnummer",
                                ClaimDataType.STRING,
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata(NAMESPACE,
                                "fornavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "etternavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "mellomnavn",
                                ClaimDataType.STRING,
                                Map.of("no", "Mellomnavn"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "pasienten_kjorer_bil",
                                ClaimDataType.STRING,
                                Map.of("no", "Pasienten kjører bil selv"),
                                true,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata(NAMESPACE,
                                "behov",
                                ClaimDataType.STRING,
                                Map.of("no", "Behov"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata(NAMESPACE,
                                "bor_ha_forerkort",
                                ClaimDataType.STRING,
                                Map.of("no", "Pasienten bør ha førerkort"),
                                false,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata(NAMESPACE,
                                "beskrivelse",
                                ClaimDataType.STRING,
                                Map.of("no", "Beskrivelse"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,1000}$"),
                        new ClaimMetadata(NAMESPACE,
                                "hjelpemidler",
                                ClaimDataType.STRING,
                                Map.of("no", "Hjelpemidler"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_med_hjelpemiddel",
                                ClaimDataType.STRING,
                                Map.of("no", "Gangdistanse med hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_uten_hjelpemiddel",
                                ClaimDataType.STRING,
                                Map.of("no", "Gangdistanse uten hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_prognose",
                                ClaimDataType.STRING,
                                Map.of("no", "Gangdistanse prognose"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$")
                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata(CredentialMetadataContext credentialMetadataContext) {
        return documentMetadata;
    }

    @Override
    public CredentialData push(PreAuthorizedIssuanceContext issuanceContext, CredentialData credentialData) {
        return credentialData;
    }

}
