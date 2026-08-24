package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Objects;


public record TrustedEntity(
        @JsonProperty("TrustedEntityInformation")
        TrustedEntityInformation trustedEntityInformation,
        @JsonProperty("TrustedEntityServices")
        @Valid List<TrustedEntityService> trustedEntityServices) {


    public boolean noneContainServiceStatus() {
        return trustedEntityServices().stream().map(TrustedEntityService::serviceInformation).map(ServiceInformation::serviceStatus).allMatch(Objects::isNull);
    }
}
