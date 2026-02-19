package no.idporten.eudiw.bevisgenerator.integration.byobservice;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.*;

import java.util.List;
import java.util.Map;

public class CredentialDefinitionFactory {

    public static CredentialDefinition empty() {
        return new CredentialDefinition(
                "your-credential-type (automatically generated)",
                "dc+sd-jwt", // default format for credential type
                "eudiw:eidas2sandkasse:dynamicvc", // default scope for credential type
                new ExampleCredentialData(Map.of(
                        "family_name", "Normann",
                        "given_name", "Kari"
                )),
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

