package no.idporten.eudiw.verifier.trustlist.etsi612;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TLService(
        @JacksonXmlProperty(localName = "ServiceName", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull TSName name,

        @JacksonXmlProperty(localName = "ServiceTypeIdentifier", namespace = EtsiNamespaces.ETSI_TSL)
        @NotEmpty String serviceTypeIdentifier,

        @JacksonXmlProperty(localName = "ServiceDigitalIdentity", namespace = EtsiNamespaces.ETSI_TSL)
        @Valid @NotNull ServiceDigitalIdentity serviceDigitalIdentity,

        @JacksonXmlProperty(localName = "ServiceStatus", namespace = EtsiNamespaces.ETSI_TSL)
        @NotEmpty URI serviceStatus
)
{
    public static final String SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS = "http://uri.etsi.org/Svc/Svctype/CA/RPaccess";
    public static final String SERVICE_TYPE_IDENTIFIER_URI_EAA = "http://uri.etsi.org/TrstSvc/Svctype/EAA";
    public static final String SERVICE_TYPE_IDENTIFIER_URI_PID = "http://uri.etsi.org/TrstSvc/Svctype/PID"; // from digdir, not from TL spec since not specified there yet.

    public static final List<String> SUPPORTED_SERVICE_TYPE_IDENTIFIERS = List.of(
            SERVICE_TYPE_IDENTIFIER_URI_RP_ACCESS,
            SERVICE_TYPE_IDENTIFIER_URI_EAA,
            SERVICE_TYPE_IDENTIFIER_URI_PID
    );

    public static final String SERVICE_STATUS_URI = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel";
    public static final String SERVICE_STATUS_URI_NATIONALLEVEL_DEPRECATED = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/deprecatedatnationallevel";
    public static final String SERVICE_STATUS_URI_WITHDRAWN = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/withdrawn";

    public TLService {

        if (!SUPPORTED_SERVICE_TYPE_IDENTIFIERS.contains(serviceTypeIdentifier)) {
            throw new IllegalArgumentException("Service: Unsupported Service Type Identifier '%s'".formatted(serviceTypeIdentifier));
        }

    }

    public boolean serviceCurrentStatus() {
        if(serviceStatus.equals(URI.create(SERVICE_STATUS_URI_NATIONALLEVEL_DEPRECATED)) || serviceStatus.equals(URI.create(SERVICE_STATUS_URI_WITHDRAWN))) {
            return false;
        }
        return true;
    }
}