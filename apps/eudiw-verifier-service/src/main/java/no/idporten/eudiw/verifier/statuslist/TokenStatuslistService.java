package no.idporten.eudiw.verifier.statuslist;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.SignedJWT;
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

    public ValidationStatus lookupStatusFromStatuslist(URI uri, int idx, StatusListTokenFormat preferredFormat) {
        ValidationStatus status;
        try {
            if (uri == null || preferredFormat == null) {
                throw new VerificationException("invalid_request", "Status list URI and preferred format are required");
            }
            StatusListResponse response = requestStatusList(uri, preferredFormat);
            if (response.body() == null || response.body().length == 0) {
                throw new VerificationException("Invalid response", "Response body from status API is empty for url %s".formatted(uri));
            }
            if (response.contentType() == null) {
                throw new VerificationException("Invalid response", "Content-Type is missing from status API response for url %s".formatted(uri));
            }
            if (hasContentType(response.contentType(), StatusListTokenFormat.JWT)) {
                return checkStatus(uri, idx, new String(response.body(), StandardCharsets.UTF_8), Instant.now());
            }
            if (hasContentType(response.contentType(), StatusListTokenFormat.CWT)) {
                return checkStatus(uri, idx, response.body(), Instant.now());
            }
            throw new VerificationException("Invalid response", "Unsupported status list Content-Type %s".formatted(response.contentType()));
        } catch (StatusCommunicationException | IOConnectionException e) {
            // TODO: create and update metrics for IOConnectionException.
            log.error("Set validation-status=INCONCLUSIVE since error in communication with status-api: {}", uri, e);
            status = ValidationStatus.INCONCLUSIVE;
        }
        return status;
    }

    private boolean hasContentType(MediaType contentType, StatusListTokenFormat format) {
        return contentType.getType().equalsIgnoreCase(format.mediaType().getType())
                && contentType.getSubtype().equalsIgnoreCase(format.mediaType().getSubtype());
    }

    private StatusListResponse requestStatusList(URI uri, StatusListTokenFormat preferredFormat) {
        if (uri == null) {
            throw new VerificationException("Invalid response", "Statuslist url is null");
        }
        StatusListTokenFormat[] formats = {preferredFormat, preferredFormat.fallback()};
        for (int index = 0; index < formats.length; index++) {
            try {
                ResponseEntity<byte[]> response = restClient.get()
                        .uri(uri)
                        .accept(formats[index].mediaType())
                        .retrieve()
                        .toEntity(byte[].class);
                return new StatusListResponse(response.getHeaders().getContentType(), response.getBody());
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_ACCEPTABLE && index < formats.length - 1) {
                    continue;
                }
                throw new StatusCommunicationException(
                        "Could not verify status",
                        "Status API returned HTTP %s for %s".formatted(e.getStatusCode(), uri),
                        e);
            } catch (ResourceAccessException e) {
                throw new IOConnectionException("Could not verify status", "Error in connection with status API " + e.getMessage(), e);
            } catch (Exception e) {
                throw new StatusCommunicationException("Could not verify status", "Error in communication with status API " + e.getMessage(), e);
            }
        }
        throw new StatusCommunicationException(
                "Could not verify status",
                "No supported status list format was returned for " + uri,
                new IllegalStateException("Both requested status-list formats were rejected"));
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
