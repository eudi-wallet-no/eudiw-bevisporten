package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.verifier.IOConnectionException;
import no.idporten.eudiw.verifier.StatusCommunicationException;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Responsible for checking revocation status. Calls Status list API with
 * status list specified inside the proof, gets the list and checks the
 * status on the index that is inside the proof.
 *
 */

@Service
public class TokenStatuslistService {
    private final RestClient restClient;
    private final TokenStatuslistConfig tokenStatuslistConfig;

    public TokenStatuslistService(RestClient restClient, TokenStatuslistConfig tokenStatuslistConfig) {
        this.restClient = restClient;
        this.tokenStatuslistConfig = tokenStatuslistConfig;
    }

    public ValidationStatus checkStatus(URI uri, int idx, String statusListJwt, Instant now) {
        if (uri == null || !StringUtils.hasText(uri.toString())) {
            return ValidationStatus.INVALID;
        }
        StatusListJwtValidator statusListJwtValidator = new StatusListJwtValidator(Set.of(JWSAlgorithm.ES256, JWSAlgorithm.RS256), tokenStatuslistConfig.clockSkew());
        JWSVerifier jwsVerifier = statusListJwsVerifier(statusListJwt);
        return convertIntStatusToEnum(statusListJwtValidator.validateAndResolveStatus(uri, idx, statusListJwt, now, jwsVerifier));
    }

    private JWSVerifier statusListJwsVerifier(String statusListJwt) {
        final SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(statusListJwt);
        } catch (ParseException e) {
            throw new VerificationException("invalid_request", "Status list JWT is not a valid compact signed JWT", e);
        }
        JWSHeader jwsHeader = jwt.getHeader();
        List<Base64> x5c = jwsHeader.getX509CertChain();
        if (x5c == null || x5c.isEmpty()) {
            throw new VerificationException("invalid_request", "Status list JWT must include x5c certificate chain");
        }

        final byte[] encodedCert;
        try {
            encodedCert = x5c.getFirst().decode();
        } catch (Exception e) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate could not be parsed", e);
        }

        X509Certificate cert = X509CertUtils.parse(encodedCert);
        if (cert == null) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate could not be parsed");
        }
        PublicKey publicKey = cert.getPublicKey();
        if (!(publicKey instanceof RSAPublicKey || publicKey instanceof ECPublicKey)) {
            throw new VerificationException("invalid_request", "Status list JWT x5c certificate must contain an EC or RSA public key");
        }
        try {
            return new DefaultJWSVerifierFactory().createJWSVerifier(jwsHeader, publicKey);
        } catch (JOSEException e) {
            throw new VerificationException("invalid_request", "Failed to create JWS verifier for Status list JWT", e);
        }
    }

    protected ValidationStatus convertIntStatusToEnum(int status) {
        if (status == 0) {
            return ValidationStatus.VALID;
        } else {
            return ValidationStatus.INVALID;
        }
    }

    public JWT requestStatusList(URI url) {
        String jwt;
        if (url != null) {
            try {
                jwt = restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);
            } catch (ResourceAccessException e) {
                throw new IOConnectionException("Could not verify status", "Error in connection with with status api " + e.getMessage(), e);
            } catch (Exception e) {
                throw new StatusCommunicationException("Could not verify status", "Error in communication with status api " + e.getMessage(), e);
            }
        } else {
            throw new VerificationException("Invalid response", "Statuslist url is null for url " + url);
        }
        if (jwt != null) {
            try {
                return SignedJWT.parse(jwt);
            } catch (ParseException e) {
                throw new VerificationException("Invalid response", "Signed statuslist JWT cannot be parsed " + url + " " + e.getMessage());
            }
        } else {
            throw new VerificationException("Invalid response", "Response body from statusAPI is null for url  " + url + " JWT is null");
        }
    }

    public ValidationStatus lookupStatusFromStatuslist(URI uri, int idx) {
        ValidationStatus status;
        try {
            status = checkStatus(
                    uri,
                    idx,
                    requestStatusList(uri).getParsedString(),
                    Instant.now());
        } catch (StatusCommunicationException | IOConnectionException e) {
            // TODO: create and update metrics for IOConnectionException.
            status = ValidationStatus.INCONCLUSIVE;
        }
        return status;
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "Statusliste: validering feila";
            case VALID:
                return "Statusliste: bevis er gyldig";
            case INVALID:
                return "Statusliste: bevis er revokert";
            case NOT_APPLICABLE:
                return "Bevis er ikke revokerbart";
            default:
                return "Statusliste: ukjent status";
        }
    }
}
