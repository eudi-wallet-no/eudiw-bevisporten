package no.idporten.eudiw.issuer.ui.demo.byob;

import no.idporten.eudiw.issuer.ui.demo.byob.model.*;

import java.util.List;

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
                                        "Namn på mitt bevis",
                                        "no",
                                        null,
                                        null
                                )
                        ),
                        List.of(
                                new Claim(
                                        "family_name",
                                        "string",
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
                                        "string",
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

