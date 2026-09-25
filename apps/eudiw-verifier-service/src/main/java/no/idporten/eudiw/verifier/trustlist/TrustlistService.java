package no.idporten.eudiw.verifier.trustlist;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.nimbusds.jose.JWSObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntity;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntityService;
import no.idporten.eudiw.verifier.trustlist.etsi602.LoTEJson;
import no.idporten.eudiw.verifier.trustlist.etsi602xml.DigitalId602Xml;
import no.idporten.eudiw.verifier.trustlist.etsi602xml.LoTEXml602;
import no.idporten.eudiw.verifier.trustlist.etsi602xml.TrustedEntity602Xml;
import no.idporten.eudiw.verifier.trustlist.etsi602xml.TrustedEntityService602Xml;
import no.idporten.eudiw.verifier.trustlist.etsi612.DigitalId;
import no.idporten.eudiw.verifier.trustlist.etsi612.LoTEXml;
import no.idporten.eudiw.verifier.trustlist.etsi612.TLServiceProvider;
import no.idporten.eudiw.verifier.trustlist.etsi612.TSPService;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class TrustlistService {

    private static final Logger log = LoggerFactory.getLogger(TrustlistService.class);
    private final RestClient trustlistRestclient;

    private final TrustlistsProperties  trustlistsProperties;

    public TrustlistService(@Qualifier("trustlist") RestClient trustlistRestclient, TrustlistsProperties trustlistsProperties) {
        this.trustlistRestclient = trustlistRestclient;
        this.trustlistsProperties = trustlistsProperties;
    }

    private String fetchTrustlist(URI uri) {
        try {
            return trustlistRestclient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot fetch trustlist", e);
        }
    }

    /**
     * Fetches and parses the trustlist at the given reference. The reference's {@link TrustlistFormat}
     * is declared in configuration (see {@link TrustlistEntry}) and fully determines how the response
     * body is parsed; no format guessing based on the URL happens here.
     */
    public Object connectToTrustlist(TrustlistReference reference) {
        String trustlist = fetchTrustlist(reference.uri());
        return switch (reference.format()) {
            case ETSI_612_XML -> xmlListMapping(trustlist);
            case ETSI_602_XML -> xml602Mapping(trustlist);
            case ETSI_602_JSON -> jsonListMapping(trustlist);
        };
    }

    public List<TrustlistReference> listOfTrustlists () {
        List<TrustlistReference> trustlists = new ArrayList<>(trustlistsProperties.getAttestationTrustlists());
        trustlists.addAll(trustlistsProperties.getPidTrustlists());
        return trustlists;
    }

    protected boolean checkJson602(URI uri, X509Certificate cert)  {
        LoTEJson lote = jsonListMapping(fetchTrustlist(uri));
        boolean allActive = lote.lote().trustedEntitiesList().stream().allMatch(TrustedEntity::noneContainServiceStatus);
        for (TrustedEntity trustedEntity : lote.lote().trustedEntitiesList()) {
            for(TrustedEntityService service : trustedEntity.trustedEntityServices()) {
                for(X509Certificate individual : service.serviceInformation().serviceDigitalIdentity().certListFromStringsToCerts()) {
                    if(compareCertificates(cert, individual)) {
                        if(allActive || service.serviceInformation().serviceStatus() != null) {
                            return true;
                        } else {
                            throw new VerificationException("invalid_request", "Service "+
                                    service.serviceInformation().serviceName().getFirst().getLocalisedValue() +
                                    "  is set to inactive on trustlist," +
                                    "or is missing status field ");
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean checkXml612(URI uri, X509Certificate cert)  {
        LoTEXml lote = xmlListMapping(fetchTrustlist(uri));
        for (TLServiceProvider sp : lote.serviceProviderList().trustServiceProviders()) {
            for(TSPService service : sp.services().services()) {
                for(DigitalId digitalId : service.serviceInformation().serviceDigitalIdentity().getCertificateDigitalIds()) {
                    if (compareCertificates(cert,digitalId.getCertificateAsX509Object()) && service.serviceInformation().serviceCurrentStatus()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean checkXml602(URI uri, X509Certificate cert)  {
        LoTEXml602 lote = xml602Mapping(fetchTrustlist(uri));
        for (TrustedEntity602Xml trustedEntity : lote.trustedEntitiesList().trustedEntities()) {
            for (TrustedEntityService602Xml service : trustedEntity.trustedEntityServices().services()) {
                for (DigitalId602Xml digitalId : service.serviceInformation().serviceDigitalIdentity().getCertificateDigitalIds()) {
                    // No status concept exists in this XML format; every entry is treated as active.
                    if (compareCertificates(cert, digitalId.getCertificateAsX509Object())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ValidationStatus checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert)  {
        return checkIfCertificateFromJwsHeaderIsOnTrustlist(cert, listOfTrustlists());
    }

    public ValidationStatus checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert, TrustlistReference trustlist) {
        return checkIfCertificateFromJwsHeaderIsOnTrustlist(cert, List.of(trustlist));
    }

    public ValidationStatus checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert, List<TrustlistReference> trustlists) {
        for (TrustlistReference trustlist : trustlists) {
            URI uri = trustlist.uri();
            boolean valid = switch (trustlist.format()) {
                case ETSI_612_XML -> checkXml612(uri, cert);
                case ETSI_602_XML -> checkXml602(uri, cert);
                case ETSI_602_JSON -> checkJson602(uri, cert);
            };
            if (valid) {
                return ValidationStatus.VALID;
            }
        }
        return ValidationStatus.INVALID;
    }

    public LoTEXml xmlListMapping(String trustlist) {
        try {
            XmlMapper xmlMapper = XmlMapper.builder()
                    .defaultUseWrapper(false)
                    .enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL)
                    .build();
            return xmlMapper.readValue(trustlist, LoTEXml.class);
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot parse ETSI TS 119 612 trustlist", e);
        }
    }

    public LoTEXml602 xml602Mapping(String trustlist) {
        try {
            XmlMapper xmlMapper = XmlMapper.builder()
                    .defaultUseWrapper(false)
                    .enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL)
                    .build();
            return xmlMapper.readValue(trustlist, LoTEXml602.class);
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot parse ETSI TS 119 602 XML trustlist", e);
        }
    }

    public LoTEJson jsonListMapping(String trustlist) {
        try {
            JWSObject jwt = JWSObject.parse(trustlist);
            ObjectMapper objectMapper = new JsonMapper();
            return objectMapper.readValue(jwt.getPayload().toString(), LoTEJson.class);
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot parse ETSI TS 119 602 trustlist", e);
        }

    }

    protected boolean compareCertificates(X509Certificate certificateFromWalletResponse,X509Certificate certificatesToCompareWith) {
        try {
            return Arrays.toString(certificatesToCompareWith.getTBSCertificate()).equals(Arrays.toString(certificateFromWalletResponse.getTBSCertificate()));
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot compare certificates", e);
        }
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "Tillitsliste: validering feila";
            case VALID:
                return "Tillitsliste: bevisets sertifikat er på tillitslista";
            case INVALID:
                return "Tillitsliste: bevisets sertifikat er ikke på noen av tillitslistene, eller er satt til inaktiv på tillitslista";
            default:
                return "Tillitsliste: ukjent status";
        }
    }
}
