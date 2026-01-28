package no.idporten.eudiw.issuer.claimssource.helsesektor;

import no.idporten.eudiw.issuer.claimssource.*;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
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
                                ClaimDataTypes.STRING,
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata(NAMESPACE,
                                "fornavn",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "etternavn",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "mellomnavn",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Mellomnavn"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata(NAMESPACE,
                                "pasienten_kjorer_bil",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Pasienten kjører bil selv"),
                                true,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata(NAMESPACE,
                                "behov",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Behov"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata(NAMESPACE,
                                "bor_ha_forerkort",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Pasienten bør ha førerkort"),
                                false,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata(NAMESPACE,
                                "beskrivelse",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Beskrivelse"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,1000}$"),
                        new ClaimMetadata(NAMESPACE,
                                "hjelpemidler",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Hjelpemidler"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_med_hjelpemiddel",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Gangdistanse med hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_uten_hjelpemiddel",
                                ClaimDataTypes.STRING,
                                Map.of("no", "Gangdistanse uten hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata(NAMESPACE,
                                "gangdistanse_prognose",
                                ClaimDataTypes.STRING,
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
