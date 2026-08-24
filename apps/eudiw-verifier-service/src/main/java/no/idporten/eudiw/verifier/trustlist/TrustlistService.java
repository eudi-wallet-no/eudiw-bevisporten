package no.idporten.eudiw.verifier.trustlist;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.nimbusds.jose.JWSObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntity;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntityService;
import no.idporten.eudiw.verifier.trustlist.etsi602.LoTEJson;
import no.idporten.eudiw.verifier.trustlist.etsi612.LoTEXml;
import no.idporten.eudiw.verifier.trustlist.etsi612.TLServiceProvider;
import no.idporten.eudiw.verifier.trustlist.etsi612.TSPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.security.cert.X509Certificate;
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

    public Object connectToTrustlist(URI uri)  {
        String trustlist;
        try{
            trustlist = trustlistRestclient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e){
            throw new VerificationException("invalid_request", "Cannot fetch trustlist" + e);
        }
        if (uri.toString().endsWith("xtsl")) {
            return xmlListMapping(trustlist);
        } else if (uri.toString().endsWith("jws")) {
            return jsonListMapping(trustlist);
        }
        return null;
    }

    public String issuerName(LdapName ldapName) {
        for (Rdn rdn : ldapName.getRdns()) {
            if ("O".equalsIgnoreCase(rdn.getType())) {
                return rdn.getValue().toString();
            }
        }
        return null;
    }

    public List<URI> listOfTrustlists () {
        return List.of(
                trustlistsProperties.getPersonalTrustlist().attestations(),
                trustlistsProperties.getWebuildTrustlist().attestations(),
                trustlistsProperties.getPersonalTrustlist().pid(),
                trustlistsProperties.getWebuildTrustlist().pid()
        );
    }

    protected boolean checkJson602(URI uri, X509Certificate cert, String jwsHeaderCertificateIssuer)  {
        LoTEJson lote = (LoTEJson) connectToTrustlist(uri);
        boolean allActive = lote.lote().trustedEntitiesList().stream().allMatch(TrustedEntity::noneContainServiceStatus);
        for (TrustedEntity trustedEntity : lote.lote().trustedEntitiesList()) {
            for(TrustedEntityService service : trustedEntity.trustedEntityServices()) {
                for(X509Certificate individual : service.serviceInformation().serviceDigitalIdentity().certListFromStringsToCerts()) {
                    String trustListIssuer = issuerName(ldapName(individual.getIssuerX500Principal().getName(X500Principal.RFC2253)));
                    if(jwsHeaderCertificateIssuer.equals(trustListIssuer)) {
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
        }
        return false;
    }

    protected boolean checkXml612(URI uri, X509Certificate cert, String jwsHeaderCertificateIssuer)  {
        LoTEXml lote = (LoTEXml) connectToTrustlist(uri);
        for (TLServiceProvider sp : lote.serviceProviderList().trustServiceProviders()) {
            for(TSPService service : sp.services().services()) {
                String trustlistIssuer = issuerName(ldapName(service.serviceInformation().serviceDigitalIdentity().
                        digitalIds().get(1).getCertificateAsX509Object().getIssuerX500Principal(). // Cert is at place 1
                        getName(X500Principal.RFC2253)));
                if (jwsHeaderCertificateIssuer.equals(trustlistIssuer)) {
                    if (compareCertificates(cert,service.serviceInformation().serviceDigitalIdentity().digitalIds()
                            .get(1).getCertificateAsX509Object()) && service.serviceInformation().serviceCurrentStatus()) {
                            return true;
                        }
                    }
                }
            }
        return false;
    }

    public boolean checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert)  {
        String jwsHeaderCertificateIssuer = issuerName(ldapName(cert.getIssuerX500Principal().getName(X500Principal.RFC2253)));
        for(URI uri : listOfTrustlists()){
            if (uri.toString().endsWith("xtsl")) {
                if (checkXml612(uri, cert, jwsHeaderCertificateIssuer)) {
                    return true;
                }
            } else if (uri.toString().endsWith("jws")) {
                if (checkJson602(uri, cert, jwsHeaderCertificateIssuer)) {
                    return true;
                }
            }
            else {
                throw new VerificationException("invalid_request", "Unknown trustlist format for trustlist " + uri);
            }
        }
        return false;
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
        return certificatesToCompareWith.getPublicKey().equals(certificateFromWalletResponse.getPublicKey());
    }

    protected LdapName ldapName(String distinguishedName) {
        try {
            return new LdapName(distinguishedName);
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Cannot parse distinguished name", e);
        }
    }
}
