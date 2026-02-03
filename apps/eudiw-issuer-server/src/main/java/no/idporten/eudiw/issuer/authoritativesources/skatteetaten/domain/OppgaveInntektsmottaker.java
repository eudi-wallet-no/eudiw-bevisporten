package no.idporten.eudiw.issuer.authoritativesources.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OppgaveInntektsmottaker(List<InntektsOpplysninger> inntektsOpplysninger) {

}
