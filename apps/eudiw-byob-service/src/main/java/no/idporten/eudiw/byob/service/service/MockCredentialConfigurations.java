package no.idporten.eudiw.byob.service.service;

import no.idporten.eudiw.byob.service.model.*;

import java.util.List;

public class MockCredentialConfigurations {


    private static final CredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_1 = new CredentialConfiguration(
            "net.eidas2sandkasse:dynamic:1_sd_jwt_vc",
            "net.eidas2sandkasse:dynamic:1",
            "dc+sd-jwt",
            List.of(new ExampleCredentialData("name", "petra")),
            new CredentialMetadata(
                    List.of(
                            new Display("Bring ditt eget bevis 1", "no", "", ""),
                            new Display("Bring your own bevis 1", "en", "", "")
                    ),
                    List.of(
                            new Claims("name",
                                    "string",
                                    null,
                                    true,
                                    List.of(new Display("Navn", "no", "", "")
                                            , new Display("Name", "en", "", ""))
                            )
                    )
            )
    );

    private static final CredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_2 = new CredentialConfiguration(
            "net.eidas2sandkasse:dynamic:2_sd_jwt_vc",
            "net.eidas2sandkasse:dynamic:2",
            "dc+sd-jwt",
            List.of(new ExampleCredentialData("age", "10")),
            new CredentialMetadata(
                    List.of(
                            new Display("Bring ditt eget bevis 2", "no", "", "")
                    ),
                    List.of(
                            new Claims("age",
                                    null,
                                    null,
                                    true,
                                    List.of(new Display("Alder", "no", "", ""),
                                            new Display("Age", "en", "", ""))
                            )
                    )
            )
    );

    public static List<CredentialConfiguration> getCredentialConfigurationsListMocked() {
        return List.of(DYNAMIC_CREDENTIAL_CONFIGURATION_1, DYNAMIC_CREDENTIAL_CONFIGURATION_2);
    }
}
