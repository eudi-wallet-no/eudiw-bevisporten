package no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public record Address(

        @JacksonXmlProperty(isAttribute = true, localName = "lang", namespace = EtsiNamespaces.ETSI_TSL)
        String lang,

        @JacksonXmlProperty(localName = "StreetAddress", namespace = EtsiNamespaces.ETSI_TSL)
        @NotBlank String streetAddress,

        @JacksonXmlProperty(localName = "Locality", namespace = EtsiNamespaces.ETSI_TSL)
        @NotBlank String locality,

        @JacksonXmlProperty(localName = "PostalCode", namespace = EtsiNamespaces.ETSI_TSL)
        @NotBlank String postalCode,

        @JacksonXmlProperty(localName = "CountryName", namespace = EtsiNamespaces.ETSI_TSL)
        @NotBlank String countryName

) {}
