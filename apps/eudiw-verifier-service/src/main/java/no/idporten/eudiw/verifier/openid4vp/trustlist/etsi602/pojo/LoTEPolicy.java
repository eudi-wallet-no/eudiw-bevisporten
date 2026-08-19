package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi602.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.Localized;

public record LoTEPolicy(@JsonProperty("LoTEPolicy") Localized loTEPolicy) {
}
