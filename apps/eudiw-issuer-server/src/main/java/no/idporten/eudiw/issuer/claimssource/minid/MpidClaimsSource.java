package no.idporten.eudiw.issuer.claimssource.minid;

import com.nimbusds.jwt.JWT;
import no.idporten.eudiw.issuer.claimssource.AbstractPreAuthorizedClaimsSource;
import no.idporten.eudiw.issuer.claimssource.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MpidClaimsSource extends AbstractPreAuthorizedClaimsSource {

    private final DocumentMetadata documentMetadata;

    public MpidClaimsSource() {
        this.documentMetadata = new DocumentMetadata(
                Map.of("no", "MinID PID"),
                List.of(
                        new ClaimMetadata("personal_administrative_number",
                                Map.of("no", "Norsk identitetsnummer"),
                                true,
                                "^\\d{11}$"),
                        new ClaimMetadata("family_name",
                                Map.of("no", "Etternavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("given_name",
                                Map.of("no", "Fornavn"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("birth_date",
                                Map.of("no", "Fødselsdato"),
                                true,
                                "^[\\x20-\\x7EæøåÆØÅ]{1,155}$"),
                        new ClaimMetadata("expiry_date",
                                Map.of("no", "Gyldig til"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$"),
                        new ClaimMetadata("issuance_date",
                                Map.of("no", "Ustedt"),
                                true,
                                "^\\d{4}-\\d{2}-\\d{2}$")
                )
        );
    }

    @Override
    protected DocumentMetadata getDocumentMetadata() {
        return documentMetadata;
    }

    @Override
    public Map<String, String> push(String issuanceTransactionId, JWT accessToken, Map<String, String> claims) {
        return claims;
    }

}
