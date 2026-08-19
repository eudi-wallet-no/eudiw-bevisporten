package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ServiceInformation(
        @JsonProperty("ServiceName")
        @Valid @NotNull List<LocalizedString> serviceName,
        @JsonProperty("ServiceDigitalIdentity")
        @Valid @NotNull ServiceDigitalIdentity serviceDigitalIdentity,
        @JsonProperty("ServiceTypeIdentifier")
        @NotNull String serviceTypeIdentifier
        ) {
}
