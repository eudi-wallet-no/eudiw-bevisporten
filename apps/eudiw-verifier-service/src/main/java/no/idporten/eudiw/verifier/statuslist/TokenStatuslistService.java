package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
import no.idporten.eudiw.verifier.ErrorCode;
import no.idporten.eudiw.verifier.IOConnectionException;
import no.idporten.eudiw.verifier.StatusCommunicationException;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
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
    private final StatusListCwtValidator statusListCwtValidator;

    private static final String STATUS_LIST_USER_ERROR = "Error in communication about status of credential";

    private static final Logger log = LoggerFactory.getLogger(TokenStatuslistService.class);

    public TokenStatuslistService(@Qualifier("tokenStatuslist") RestClient restClient, TokenStatuslistConfig tokenStatuslistConfig) {
        this.restClient = restClient;
        this.tokenStatuslistConfig = tokenStatuslistConfig;
        this.statusListCwtValidator = new StatusListCwtValidator(tokenStatuslistConfig.clockSkew());
    }

    public ValidationStatus checkStatus(URI uri, int idx, String statusListJwt, Instant now) {
        if (uri == null || !StringUtils.hasText(uri.toString())) {
            return ValidationStatus.INVALID;
        }
        StatusListJwtValidator statusListJwtValidator = new StatusListJwtValidator(Set.of(JWSAlgorithm.ES256, JWSAlgorithm.RS256), tokenStatuslistConfig.clockSkew());
        JWSVerifier jwsVerifier = statusListJwsVerifier(statusListJwt);
        return convertIntStatusToEnum(statusListJwtValidator.validateAndResolveStatus(uri, idx, statusListJwt, now, jwsVerifier));
    }

    public ValidationStatus checkStatus(URI uri, int idx, byte[] statusListCwt, Instant now) {
        if (uri == null || !StringUtils.hasText(uri.toString())) {
            return ValidationStatus.INVALID;
        }
        return convertIntStatusToEnum(statusListCwtValidator.validateAndResolveStatus(uri, idx, statusListCwt, now));
    }

    private String keyReferenceHeaders(JWSHeader header) {
        List<String> found = new ArrayList<>();
        if (header.getKeyID() != null) found.add("kid");
        if (header.getX509CertURL() != null) found.add("x5u");
        if (header.getX509CertThumbprint() != null) found.add("x5t");
        if (header.getX509CertSHA256Thumbprint() != null) found.add("x5t#S256");
        if (header.getJWKURL() != null) found.add("jku");
        if (header.getJWK() != null) found.add("jwk");
        return String.join("/", found);
    }

    private JWSVerifier statusListJwsVerifier(String statusListJwt) {
        final SignedJWT jwt;
        try {
            jwt = SignedJWT.parse(statusListJwt);
        } catch (ParseException e) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT is not a valid compact signed JWT", e);
        }
        JWSHeader jwsHeader = jwt.getHeader();
        List<Base64> x5c = jwsHeader.getX509CertChain();
        if (x5c == null || x5c.isEmpty()) {
            String keyReferences = keyReferenceHeaders(jwsHeader);
            if (!keyReferences.isEmpty()) {
                throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT key resolution by " + keyReferences
                        + " is not supported yet, x5c is required");
            }
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT must include x5c certificate chain");
        }

        final byte[] encodedCert;
        try {
            encodedCert = x5c.getFirst().decode();
        } catch (Exception e) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT x5c certificate could not be parsed", e);
        }

        X509Certificate cert = X509CertUtils.parse(encodedCert);
        if (cert == null) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT x5c certificate could not be parsed");
        }
        PublicKey publicKey = cert.getPublicKey();
        if (!(publicKey instanceof RSAPublicKey || publicKey instanceof ECPublicKey)) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list JWT x5c certificate must contain an EC or RSA public key");
        }
        try {
            return new DefaultJWSVerifierFactory().createJWSVerifier(jwsHeader, publicKey);
        } catch (JOSEException e) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Failed to create JWS verifier for Status list JWT", e);
        }
    }

    protected ValidationStatus convertIntStatusToEnum(int status) {
        if (status == 0) {
            return ValidationStatus.VALID;
        } else {
            return ValidationStatus.INVALID;
        }
    }

    public ValidationStatus lookupStatusFromStatuslist(URI uri, int idx, StatusListTokenFormat preferredFormat) {
        if (uri == null || preferredFormat == null) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Status list URI and preferred format are required");
        }
        StatusListResponse response = requestStatusList(uri, preferredFormat);
        if (response.body() == null || response.body().length == 0) {
            throw new StatusCommunicationException(
                    ErrorCode.INVALID_REQUEST, STATUS_LIST_USER_ERROR,
                    "Response body from status API is empty for url %s".formatted(uri),
                    null);
        }
        if (response.contentType() == null) {
            throw new StatusCommunicationException(
                    ErrorCode.INVALID_REQUEST, STATUS_LIST_USER_ERROR,
                    "Content-Type is missing from status API response for url %s".formatted(uri),
                    null);
        }
        if (hasContentType(response.contentType(), StatusListTokenFormat.JWT)) {
            return checkStatus(uri, idx, new String(response.body(), StandardCharsets.UTF_8), Instant.now());
        }
        if (hasContentType(response.contentType(), StatusListTokenFormat.CWT)) {
            return checkStatus(uri, idx, response.body(), Instant.now());
        } else {
            throw new StatusCommunicationException(
                    ErrorCode.INVALID_REQUEST,
                    STATUS_LIST_USER_ERROR,
                    "Unsupported status list Content-Type %s".formatted(response.contentType()),
                    null);
        }
    }

    private boolean hasContentType(MediaType contentType, StatusListTokenFormat format) {
        return contentType.getType().equalsIgnoreCase(format.mediaType().getType())
                && contentType.getSubtype().equalsIgnoreCase(format.mediaType().getSubtype());
    }

    private StatusListResponse requestStatusList(URI uri, StatusListTokenFormat preferredFormat) {
        if (uri == null) {
            throw new VerificationException(ErrorCode.INVALID_REQUEST, "Statuslist url is null");
        }
        StatusListTokenFormat[] formats = {preferredFormat, preferredFormat.fallback()};
        for (int index = 0; index < formats.length; index++) {
            try {

                ResponseEntity<byte[]> response = restClient.get()
                        .uri(uri)
                        .accept(formats[index].mediaType())
                        .retrieve()
                        .toEntity(byte[].class);
                log.info("Status list {} supports format {}. Response: {}.", uri, formats[index].mediaType(), response.getStatusCode());
                return new StatusListResponse(response.getHeaders().getContentType(), response.getBody());
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_ACCEPTABLE && index < formats.length - 1) {
                    log.warn("Status list {} does not support format {}.", uri, formats[index].mediaType());
                    continue;
                }
                throw new StatusCommunicationException(
                        ErrorCode.INVALID_REQUEST, STATUS_LIST_USER_ERROR,
                        "Status list API returned status code=%s for uri=%s".formatted(e.getStatusCode(), uri),
                        e);
            } catch (ResourceAccessException e) {
                throw new IOConnectionException("Error in connection with status list API %s".formatted(uri), e);
            } catch (Exception e) {
                throw new StatusCommunicationException(ErrorCode.INVALID_REQUEST, STATUS_LIST_USER_ERROR, "Error in communication with status list API=" + uri, e);
            }
        }
        throw new StatusCommunicationException(
                ErrorCode.INVALID_REQUEST,
                STATUS_LIST_USER_ERROR,
                "No supported status list format was returned for uri=" + uri,
                null);
    }

    private record StatusListResponse(MediaType contentType, byte[] body) {
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        return switch (status) {
            case INCONCLUSIVE -> "Statusliste: validering feila";
            case VALID -> "Statusliste: bevis er gyldig";
            case INVALID -> "Statusliste: bevis er revokert";
            case NOT_APPLICABLE -> "Statusliste: bevis er ikke revokerbart";
            default -> "Statusliste: ukjent status";
        };
    }
}
