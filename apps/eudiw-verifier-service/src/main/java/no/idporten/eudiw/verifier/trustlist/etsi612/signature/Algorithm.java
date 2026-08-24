package no.idporten.eudiw.verifier.trustlist.etsi612.signature;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import jakarta.validation.constraints.NotBlank;

public class Algorithm {

    @JacksonXmlProperty(localName = "Algorithm", isAttribute = true)
    @NotBlank
    private String algorithm;

    @JacksonXmlText
    @NotBlank
    private String value;

    public Algorithm() {
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

