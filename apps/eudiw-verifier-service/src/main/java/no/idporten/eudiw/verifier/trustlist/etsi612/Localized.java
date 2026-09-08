package no.idporten.eudiw.verifier.trustlist.etsi612;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import jakarta.validation.constraints.NotBlank;

public class Localized {

    @JacksonXmlProperty(localName = "lang", isAttribute = true)
    @NotBlank
    private String lang;

    @JacksonXmlText
    @NotBlank
    private String value;

    public Localized() {
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
