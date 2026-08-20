package no.idporten.eudiw.verifier.trustlist.etsi602.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.idporten.eudiw.verifier.trustlist.etsi612.Localized;

public record LoTELegalNotice(@JsonProperty("LoTELegalNotice") Localized loTELegalNotice) {
}
