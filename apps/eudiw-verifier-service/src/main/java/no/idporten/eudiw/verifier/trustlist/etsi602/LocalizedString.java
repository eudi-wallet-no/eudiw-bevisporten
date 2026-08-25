package no.idporten.eudiw.verifier.trustlist.etsi602;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LocalizedString {
    @JsonProperty("lang")
    private String lang;

    @JsonProperty("value")
    private String value;

   public LocalizedString() {
   }

   public String getLang() {
       return lang;
   }

   public String getLocalisedValue() {
       return value;
   }

   public void setLang(String lang) {
       this.lang = lang;
   }

   public void setValue(String value) {
       this.value = value;
   }
}