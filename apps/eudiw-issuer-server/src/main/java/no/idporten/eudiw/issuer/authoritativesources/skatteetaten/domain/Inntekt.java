package no.idporten.eudiw.issuer.authoritativesources.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Inntekt(Double beloep) {
}
