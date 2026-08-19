package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TrustedEntityInformation(
        @NotBlank List<LocalizedString> teName,
        @NotBlank List<LocalizedString> teTradeName,
        @Valid @NotNull InformationUri informationUri,
        TeAddress teAddress // this exists for pid phone number
        ) {
}
