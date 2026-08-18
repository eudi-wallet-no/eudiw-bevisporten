package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import no.idporten.eudiw.verifier.VerificationException;


import java.math.BigInteger;
import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TLSchemeInformation(
        @JacksonXmlProperty(localName = "SchemeOperatorName", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSName schemeName,
        @JacksonXmlProperty(localName = "TSLSequenceNumber", namespace = EtsiNamespaces.ETSI_TSL)
        @Positive @NotNull BigInteger sequenceNumber,
        @JacksonXmlProperty(localName = "ListIssueDateTime", namespace = EtsiNamespaces.ETSI_TSL)
        @NotNull String listIssueDateTime,
        @JacksonXmlProperty(localName = "SchemeInformationURI", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSUri informationUris)
{
    public TLSchemeInformation {
        if (listIssueDateTime == null || ZonedDateTime.parse(listIssueDateTime).isAfter(ZonedDateTime.now())) {
            throw new VerificationException("invalid_request","Invalid list of scheme issue dates for trustlist");
        }
    }
}