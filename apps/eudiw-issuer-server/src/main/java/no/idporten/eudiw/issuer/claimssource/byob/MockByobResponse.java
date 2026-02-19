package no.idporten.eudiw.issuer.claimssource.byob;

import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicClaimMetadata;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfiguration;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialConfigurations;
import no.idporten.eudiw.issuer.claimssource.byob.domain.DynamicCredentialMetadata;
import no.idporten.eudiw.issuer.credentials.types.DocumentMetadata;
import no.idporten.eudiw.issuer.credentials.formats.CredentialFormat;

import java.util.List;

public class MockByobResponse {
    private static final DynamicCredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_1 = DynamicCredentialConfiguration.builder()
            .credentialConfigurationId("net.eidas2sandkasse:dynamic:1_sd_jwt_vc")
            .credentialType("dynamic:1")
            .format(CredentialFormat.SD_JWT_VC.formatIdentifier())
            .credentialMetadata(new DynamicCredentialMetadata(
                            List.of(
                                    new DocumentMetadata.Display("no", "Bring ditt eget bevis 1"),
                                    new DocumentMetadata.Display("en", "Bring your own bevis 1")
                            ),
                            List.of(
                                    new DynamicClaimMetadata("name",
                                            List.of(
                                                    new DocumentMetadata.Display("no", "Navn"),
                                                    new DocumentMetadata.Display("en", "Name")),
                                            true,
                                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
                            )
                    )
            )
            .build();

    private static final DynamicCredentialConfiguration DYNAMIC_CREDENTIAL_CONFIGURATION_2 = DynamicCredentialConfiguration.builder()
            .credentialConfigurationId("net.eidas2sandkasse:dynamic:2_sd_jwt_vc")
            .credentialType("dynamic:2")
            .format(CredentialFormat.SD_JWT_VC.formatIdentifier())
            .credentialMetadata(new DynamicCredentialMetadata(
                            List.of(
                                    new DocumentMetadata.Display("no", "Bring ditt eget bevis 2")
                            ),
                            List.of(
                                    new DynamicClaimMetadata("age",
                                            List.of(
                                                    new DocumentMetadata.Display("no", "Alder")),
                                            true,
                                            "^[\\x20-\\x7EæøåÆØÅ]{1,255}$")
                            )
                    )
            )
            .build();



    public static DynamicCredentialConfigurations getMockedByobResponse() {
        return DynamicCredentialConfigurations.builder()
                .credentialConfigurations(List.of(
                        DYNAMIC_CREDENTIAL_CONFIGURATION_1,
                        DYNAMIC_CREDENTIAL_CONFIGURATION_2
                ))
                .build();
    }
}
