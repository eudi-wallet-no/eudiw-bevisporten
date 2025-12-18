package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.domain.ClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.domain.DocumentMetadata;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ByobCredentialConfigurationService {

    private Map<String, DocumentMetadata> byobServiceMap = Map.of(
            "eidas2sandkasse.wildcard_sd_jwt_vc",
            new DocumentMetadata(
                    List.of(
                            new DocumentMetadata.Display("no", "Bring ditt eget bevis"),
                            new DocumentMetadata.Display("en", "Bring your own bevis")
                    ),
                    List.of(
                            new ClaimMetadata("name",
                                    Map.of(
                                            "no", " Navn",
                                            "en", "name"),
                                    true,
                                    "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
                    )
            )
    );

    public DocumentMetadata getDocumentMetadata(String credentialType) {
        return byobServiceMap.get(credentialType);
    }

}
