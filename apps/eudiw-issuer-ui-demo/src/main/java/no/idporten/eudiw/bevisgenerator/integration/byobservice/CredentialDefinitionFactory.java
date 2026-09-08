package no.idporten.eudiw.bevisgenerator.integration.byobservice;

import no.idporten.eudiw.bevisgenerator.integration.byobservice.model.*;

import java.util.List;
import java.util.Map;

public class CredentialDefinitionFactory {
    public static final String DYNAMIC_CREDENTIAL_SCOPE = "eudiw:eidas2sandkasse:dynamicvc";

    public static CredentialDefinition empty() {
        return new CredentialDefinition(
                "your-credential-type (generert automatisk)",
                "dc+sd-jwt", // default format for credential type
                DYNAMIC_CREDENTIAL_SCOPE,
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
                                                        "Etternamn",
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
                                                        "Førenamn",
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
