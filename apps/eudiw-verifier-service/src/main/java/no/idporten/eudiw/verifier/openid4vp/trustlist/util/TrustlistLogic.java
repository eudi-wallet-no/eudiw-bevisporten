package no.idporten.eudiw.verifier.openid4vp.trustlist.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.config.TrustlistsProperties;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.LoTE;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.TLServiceProvider;
import no.idporten.eudiw.verifier.openid4vp.trustlist.etsi612.TSPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.List;

public class TrustlistLogic {

    private static final Logger log = LoggerFactory.getLogger(TrustlistLogic.class);
    private RestClient trustlistRestclient;

    private TrustlistsProperties  trustlistsProperties;

    public TrustlistLogic(@Qualifier("trustlist") RestClient trustlistRestclient, TrustlistsProperties trustlistsProperties) {
        this.trustlistRestclient = trustlistRestclient;
        this.trustlistsProperties = trustlistsProperties;
    }

    public LoTE connectToTrustlist(URI uri) throws JsonProcessingException {
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

    // TODO: also include json lists

    public List<URI> listOfTrustlists () {
        return List.of(
                trustlistsProperties.getPersonalTrustlist().attestations(),
                trustlistsProperties.getWebuildTrustlist().attestations());
    }

    public boolean checkIfCertificateFromJwsHeaderIsOnTrustlist(X509Certificate cert) throws Exception {
        String jwsHeaderCertificateIssuer = issuerName(new LdapName(cert.getIssuerX500Principal().getName(X500Principal.RFC2253)));
        for(URI uri : listOfTrustlists()){
            LoTE lote = connectToTrustlist(uri);
            for (TLServiceProvider sp : lote.serviceProviderList().trustServiceProviders()) {
                String trustlistIssuer = issuerName(new LdapName(sp.services().services().getFirst().serviceInformation().serviceDigitalIdentity().digitalIds().get(1).getCertificateAsX509Object().getIssuerX500Principal().getName(X500Principal.RFC2253)));
                if (jwsHeaderCertificateIssuer.equals(trustlistIssuer)) {
                    for (TSPService service : sp.services().services()) {
                        if (java.util.Arrays.equals(cert.getPublicKey().getEncoded(), service.serviceInformation().serviceDigitalIdentity().digitalIds().get(1).getCertificateAsX509Object().getPublicKey().getEncoded())) {
                            return true;
                        }
                    }
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
}
