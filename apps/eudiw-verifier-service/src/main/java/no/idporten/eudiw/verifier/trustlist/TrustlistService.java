package no.idporten.eudiw.verifier.trustlist;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.nimbusds.jose.JWSObject;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.trustlist.etsi602.ServiceInformation;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntity;
import no.idporten.eudiw.verifier.trustlist.etsi602.TrustedEntityService;
import no.idporten.eudiw.verifier.trustlist.etsi602.pojo.LoTEResponse;
import no.idporten.eudiw.verifier.trustlist.etsi612.LoTE;
import no.idporten.eudiw.verifier.trustlist.etsi612.TLServiceProvider;
import no.idporten.eudiw.verifier.trustlist.etsi612.TSPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.List;

@Service
public class TrustlistService {

    private static final Logger log = LoggerFactory.getLogger(TrustlistService.class);
    private RestClient trustlistRestclient;

    private TrustlistsProperties  trustlistsProperties;

    public TrustlistService(@Qualifier("trustlist") RestClient trustlistRestclient, TrustlistsProperties trustlistsProperties) {
        this.trustlistRestclient = trustlistRestclient;
        this.trustlistsProperties = trustlistsProperties;
    }

    public Object connectToTrustlist(URI uri) throws JsonProcessingException, ParseException {
        String trustlist;
        try{
            trustlist = trustlistRestclient.get()
                    .uri(uri.toString())
                    .retrieve()
                    .body(String.class);
        } catch (Exception e){
            throw new VerificationException("invalid_request", e.getMessage());
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

    protected boolean compareCertificates(X509Certificate certificateFromWalletResponse, List<X509Certificate> certificatesToCompareWith) {
        for(X509Certificate certificate : certificatesToCompareWith){
            if(certificate.getPublicKey().equals(certificateFromWalletResponse.getPublicKey())){
                return true;
            }
        }
        return false;
    }

    protected boolean checkJson602(URI uri, X509Certificate cert, String jwsHeaderCertificateIssuer) throws IOException, InvalidNameException, ParseException {
        LoTEResponse lote = (LoTEResponse) connectToTrustlist(uri);
        boolean allActive = lote.lote().trustedEntitiesList().stream().allMatch(TrustedEntity::noneContainServiceStatus);
        for (TrustedEntity trustedEntity : lote.lote().trustedEntitiesList()) {
            String trustListIssuer = issuerName(new LdapName(trustedEntity.trustedEntityServices().getFirst().serviceInformation().serviceDigitalIdentity().X509Certificates().getFirst().getCertificateAsX509Object().getIssuerX500Principal().getName(X500Principal.RFC2253)));
            if (jwsHeaderCertificateIssuer.equals(trustListIssuer)) {
                for (TrustedEntityService trustedEntityService : trustedEntity.trustedEntityServices()) {
                    if(compareCertificates(cert,  trustedEntityService.serviceInformation().serviceDigitalIdentity().certListFromStringsToCerts())) {
                        if(allActive || trustedEntityService.serviceInformation().serviceStaus() != null) {
                            return true;
                        } else {
                            log.info("Certificate {} is set to not active on trustlist", cert.getSubjectX500Principal().getName());
                        }
                    }
                }
            }
        }
        return false;
    }

    protected boolean checkXml612(URI uri, X509Certificate cert, String jwsHeaderCertificateIssuer) throws IOException, CertificateException, InvalidNameException, ParseException {
        LoTE lote = (LoTE) connectToTrustlist(uri);
        for (TLServiceProvider sp : lote.serviceProviderList().trustServiceProviders()) {
            String trustlistIssuer = issuerName(new LdapName(sp.services().services().getFirst().serviceInformation().serviceDigitalIdentity().digitalIds().get(1).getCertificateAsX509Object().getIssuerX500Principal().getName(X500Principal.RFC2253)));
            if (jwsHeaderCertificateIssuer.equals(trustlistIssuer)) {
                for (TSPService service : sp.services().services()) {
                    if (compareCertificates(cert, List.of(service.serviceInformation().serviceDigitalIdentity().digitalIds().get(1).getCertificateAsX509Object()))) {
                        if (service.serviceInformation().checkServiceCurrentStatus()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert) throws Exception {
        String jwsHeaderCertificateIssuer = issuerName(new LdapName(cert.getIssuerX500Principal().getName(X500Principal.RFC2253)));
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
        }
        return false;
    }

    public LoTE xmlListMapping(String trustlist) throws JsonProcessingException {
        XmlMapper xmlMapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .enable(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL)
                .build();

        return xmlMapper.readValue(trustlist, LoTE.class);
    }

    public LoTEResponse jsonListMapping(String trustlist) throws ParseException {
        JWSObject jwt = JWSObject.parse(trustlist);
        ObjectMapper objectMapper = new JsonMapper();
        return objectMapper.readValue(jwt.getPayload().toString(), LoTEResponse.class);
    }
}
