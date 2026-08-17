package no.idporten.eudiw.connector.authoritativesources.skatteetaten.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Respons(@JsonProperty("oppgaveInntektsmottaker") List<InntektsOpplysninger> oppgaveInntektsmottaker) {
}
