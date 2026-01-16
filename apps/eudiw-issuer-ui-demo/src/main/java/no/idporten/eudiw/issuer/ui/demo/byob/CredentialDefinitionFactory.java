package no.idporten.eudiw.issuer.ui.demo.byob;

import no.idporten.eudiw.issuer.ui.demo.byob.model.*;

import java.util.List;
import java.util.Map;

public class CredentialDefinitionFactory {

    public static CredentialDefinition empty() {
        return new CredentialDefinition(
            "your-credential-configuration-name (automatically generated)",
            List.of(
                new ExampleCredentialData("family_name", "Normann"),
                new ExampleCredentialData("given_name", "Kari")
            ),
            new CredentialMetadata(
                List.of(
                    new Display(
                        "test",
                        "no",
                        null,
                        null
                    )
                ),
                List.of(
                    new Claim(
                        "family_name",
                        true,
                        List.of(
                            new Display(
                                "Etternavn",
                                "no",
                                null,
                                null
                            )
                        )
                    ),
                        new Claim(
                            "given_name",
                            true,
                            List.of(new Display(
                                "Fornavn",
                                "no",
                                null,
                                null
                           )
                        )
                    )
                )
            )
        );
    }
}

