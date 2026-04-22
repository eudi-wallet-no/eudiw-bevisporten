package no.idporten.eudiw.statuslist.provider;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.statuslist.exceptions.StatusListNotFoundException;
import no.idporten.eudiw.statuslist.exceptions.StatusListSigningException;
import no.idporten.eudiw.statuslist.service.StatusListService;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.*;

@Service
public class StatusProviderService {

    private static final String KEY_PROVIDER = "status-provider";
    private static final List<String> existingLists = List.of("1", "2", "3");
    private final StatusProviderProperties statusProviderProperties;
    private final KeystoreManager keystoreManager;
    private final StatusListService statuslistService;


    public StatusProviderService(StatusProviderProperties statusProviderProperties, KeystoreManager keystoreManager, StatusListService statuslistService) {
        this.statusProviderProperties = statusProviderProperties;
        this.keystoreManager = keystoreManager;
        this.statuslistService = statuslistService;
    }

    public JWT getStatusList(String id) {
        if (!existingLists.contains(id)) {
            throw new StatusListNotFoundException(id);
        }

        JWTClaimsSet claimsSet = buildJwtClaimsSet(id);
        return buildSignedJWT(claimsSet);
    }

    private JWTClaimsSet buildJwtClaimsSet(String id) {
        Map<String, Object> statusList = getStatusListClaims(id);

        Date issueTime = new Date();
        Date expirationTime = new Date(issueTime.getTime() + statusProviderProperties.valid().toMillis());

        URI uri = UriComponentsBuilder.fromUri(statusProviderProperties.uri())
                .pathSegment("lists", id)
                .build()
                .toUri();

        return new JWTClaimsSet.Builder()
                .issueTime(issueTime)
                .expirationTime(expirationTime)
                .claim("ttl", statusProviderProperties.ttl().toSeconds())
                .claim("status_list", statusList)
                .subject(uri.toString())
                .build();
    }

    private Map<String, Object> getStatusListClaims(String id) {
        String compressedList = statuslistService.getJsonStatuslist();
        Map<String, Object> statusList = new HashMap<>();

        statusList.put("bits", 1);
        statusList.put("lst", compressedList);

        return statusList;
    }

    private SignedJWT buildSignedJWT(JWTClaimsSet claimsSet) {
        KeyProvider keyProvider = keystoreManager.getKeyProvider(KEY_PROVIDER);

        List<Base64> x5c = getBase64CertificateChain(keyProvider);

        JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("statuslist+jwt"))
                .x509CertChain(x5c)
                .build();

        return createSignedJWT(claimsSet, jwsHeader, keyProvider);
    }

    private static List<Base64> getBase64CertificateChain(KeyProvider keyProvider) {
        Certificate certificate = keyProvider.certificate();

        Base64 x5c;
        try {
            x5c = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new StatusListSigningException("Failed to encode certificate", e);
        }

        return Collections.singletonList(x5c);
    }

    private static SignedJWT createSignedJWT(JWTClaimsSet claimsSet, JWSHeader jwsHeader, KeyProvider keyProvider) {
        SignedJWT jwt = new SignedJWT(jwsHeader, claimsSet);

        PrivateKey privateKey = keyProvider.privateKey();
        RSASSASigner signer = new RSASSASigner(privateKey);

        return sign(jwt, signer);
    }

    private static SignedJWT sign(SignedJWT jwt, RSASSASigner signer) {
        try {
            jwt.sign(signer);
        } catch (JOSEException e) {
            throw new StatusListSigningException("Failed to sign JWT", e);
        }

        return jwt;
    }
}
