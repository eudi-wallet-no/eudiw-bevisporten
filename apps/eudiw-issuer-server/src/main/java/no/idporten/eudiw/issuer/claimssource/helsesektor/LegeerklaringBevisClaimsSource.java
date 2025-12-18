package no.idporten.eudiw.issuer.claimssource.helsesektor;

import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.PreAuthorizedIssuanceContext;
import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegeerklaringBevisClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;

    public LegeerklaringBevisClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                List.of(new DocumentMetadata.Display("no", "Legeerklæringbevis","Legeerklæringbevis utstedt av kommune (Hackathon Brukerrådet 2025)")),
                List.of(
                        new ClaimMetadata("fodselsnummer",
                                Map.of("no", "Fødselsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("fornavn",
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata("etternavn",
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata("mellomnavn",
                                Map.of("no", "Mellomnavn"),
                                false,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,150}$"),
                        new ClaimMetadata("pasienten_kjorer_bil",
                                Map.of("no", "Pasienten kjører bil selv"),
                                true,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata("behov",
                                Map.of("no", "Behov"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata("bor_ha_forerkort",
                                Map.of("no", "Pasienten bør ha førerkort"),
                                false,
                                "^[jnJN]{1}$"),
                        new ClaimMetadata("beskrivelse",
                                Map.of("no", "Beskrivelse"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,1000}$"),
                        new ClaimMetadata("hjelpemidler",
                                Map.of("no", "Hjelpemidler"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$"),
                        new ClaimMetadata("gangdistanse_med_hjelpemiddel",
                                Map.of("no", "Gangdistanse med hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata("gangdistanse_uten_hjelpemiddel",
                                Map.of("no", "Gangdistanse uten hjelpemiddel (meter)"),
                                true,
                                "^\\d{1,6}$"),
                        new ClaimMetadata("gangdistanse_prognose",
                                Map.of("no", "Gangdistanse prognose"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,500}$")
                )
        );
    }

    @Override
    public DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public Map<String, Object> push(PreAuthorizedIssuanceContext issuanceContext, Map<String, String> claims) {
        return new HashMap<>(claims);
    }

}
