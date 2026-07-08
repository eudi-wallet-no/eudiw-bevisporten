package no.idporten.eudiw.bevisgenerator.integration.issuerserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IssuanceSubject(@JsonProperty("identifier") String identifier) { }
