package no.idporten.eudiw.verifier.trustlist.etsi602;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.idporten.eudiw.verifier.VerificationException;

import java.math.BigInteger;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;

public record ListAndSchemeInformation(
        @JsonProperty("SchemeName")
        @NotNull @Valid List<@NotNull LocalizedString> schemeName,
        @JsonProperty("SequenceNumber")
        @Positive @NotNull BigInteger sequenceNumber,
        @JsonProperty("ListIssueDateTime")
        @NotBlank String listIssueDateTime,
        @JsonProperty("LoteType")
        @NotNull @Valid URI loteType,
        @JsonProperty("InformationUris")
        @NotNull @Valid TSUri informationUris,
        @JsonProperty("StatusDeterminationApproach")
        @NotNull @Valid URI statusDeterminationApproach,
        @JsonProperty("SchemeTypeCommunityRules")
        @Valid @NotEmpty List<@NotNull LocalizedString> schemeTypeCommunityRules,
        @JsonProperty("SchemeOperatorAddress")
        @Valid @NotNull Address schemeOperatorAddress
        ) {
    public ListAndSchemeInformation {
        if (listIssueDateTime == null || ZonedDateTime.parse(listIssueDateTime).isAfter(ZonedDateTime.now())) {
            throw new VerificationException("invalid_request","Invalid list of scheme issue dates for trustlist");
        }
    }

}
