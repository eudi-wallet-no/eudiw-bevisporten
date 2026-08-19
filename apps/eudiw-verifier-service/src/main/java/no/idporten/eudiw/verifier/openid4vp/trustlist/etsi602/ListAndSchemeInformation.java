package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public record ListAndSchemeInformation(
        @JsonProperty("SchemeName")
        @NotNull @Valid List<LocalizedString> schemeName,
        @Positive @NotNull BigInteger sequenceNumber,
        @NotNull ZonedDateTime listIssueDateTime,
        @NotNull @Valid URI loteType,
        @NotNull @Valid TSUri informationUris,
        @NotNull @Valid URI statusDeterminationApproach,
        @NotBlank String schemeTypeCommunityRules
        ) {
    public ListAndSchemeInformation {
//        if (listIssueDateTime == null || listIssueDateTime.isAfter(ZonedDateTime.now())) {
//            throw new IllegalArgumentException("List issue date time must not be null and not in the future");
//        }
    }

}
