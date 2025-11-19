package no.idporten.eudiw.issuer.claimssource.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Inntekt(Double beloep) {
}
