package no.idporten.eudiw.issuer.authoritativesources.pid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import no.digdir.freg.domain.json.*;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(
        ignoreUnknown = true
)
@JsonPropertyOrder({"identifikasjonsnummer", "navn", "foedsel", "status"})
public class FregPerson {

    @JsonProperty("identifikasjonsnummer")
    private List<Identifikasjonsnummer> identifikasjonsnummer;

    @JsonProperty("navn")
    private List<Folkeregisterpersonnavn> navn;

    @JsonProperty("foedsel")
    private List<Folkeregisterfoedsel> foedsel;

    @JsonProperty("statsborgerskap")
    public List<Statsborgerskap> statsborgerskap;

    @JsonProperty("status")
    private List<Folkeregisterpersonstatus> status;


    public FregPerson() {
    }

    @JsonProperty("navn")
    public List<Folkeregisterpersonnavn> getNavn() {
      return navn;
    }

    @JsonProperty("foedsel")
    public List<Folkeregisterfoedsel> getFoedsel() {
       return foedsel;
    }

    @JsonProperty("identifikasjonsnummer")
    public List<Identifikasjonsnummer> getIdentifikasjonsnummer() {
       return identifikasjonsnummer;
    }

    public List<Statsborgerskap> getStatsborgerskap() {
        return this.statsborgerskap;
    }

    public List<Folkeregisterpersonstatus> getStatus() {
        return this.status;
    }

    @JsonProperty("identifikasjonsnummer")
    public void setIdentifikasjonsnummer(List<Identifikasjonsnummer> identifikasjonsnummer) {
        this.identifikasjonsnummer = identifikasjonsnummer;
    }

    @JsonProperty("navn")
    public void setNavn(List<Folkeregisterpersonnavn> navn) {
        this.navn = navn;
    }

    @JsonProperty("foedsel")
    public void setFoedsel(List<Folkeregisterfoedsel> foedsel) {
        this.foedsel = foedsel;
    }

    @JsonProperty("statsborgerskap")
    public void setStatsborgerskap(List<Statsborgerskap> statsborgerskap) {
        this.statsborgerskap = statsborgerskap;
    }

    @JsonProperty("status")
    public void setStatus(List<Folkeregisterpersonstatus> status) {
        this.status = status;
    }

}
