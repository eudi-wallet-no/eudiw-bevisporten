package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.JWK;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.api.openid4vp.WalletCallback;
import no.idporten.eudiw.verifier.logging.audit.AuditService;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialMeta;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationDetail;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationType;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.trustlist.TrustlistReference;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import no.idporten.eudiw.verifier.trustlist.TrustlistsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import org.springframework.util.StringUtils;
import org.springframework.util.CollectionUtils;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class OpenID4VPResponseService {

    private static final Logger log = LoggerFactory.getLogger(OpenID4VPResponseService.class);
    private static final String PID_DOC_TYPE = "eu.europa.ec.eudi.pid.1";
    private static final Pattern PID_VCT_PATTERN = Pattern.compile("urn:eudi:pid:(?:[a-z]{2}:)?[0-9]+", Pattern.CASE_INSENSITIVE);

    private final VerificationTransactionService verificationTransactionService;
    private final TokenStatuslistService tokenStatuslistService;
    private final TrustlistService trustlistService;
    private final TrustlistsProperties trustlistsProperties;
    private final AuditService auditService;

    private final SdJwtCredentialVerifier sdJwtCredentialVerifier;
    private final MdocCredentialVerifier mdocCredentialVerifier;

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService, TokenStatuslistService tokenStatuslistService, TrustlistService trustlistService, TrustlistsProperties trustlistsProperties, AuditService auditService, SdJwtCredentialVerifier sdJwtCredentialVerifier, MdocCredentialVerifier mdocCredentialVerifier) {
        this.verificationTransactionService = verificationTransactionService;
        this.tokenStatuslistService = tokenStatuslistService;
        this.trustlistService = trustlistService;
        this.trustlistsProperties = trustlistsProperties;
        this.auditService = auditService;
        this.sdJwtCredentialVerifier = sdJwtCredentialVerifier;
        this.mdocCredentialVerifier = mdocCredentialVerifier;
    }

    public WalletCallback receiveResponse(ClientApplication clientApplication, String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        try {
            return processResponse(
                    clientApplication,
                    verifierTransactionId,
                    encryptedAuthorizationResponse,
                    verificationTransaction);
        } catch (Exception responseException) {
            markAsError(clientApplication, verifierTransactionId, responseException);
            throw responseException;
        }
    }

    private WalletCallback processResponse(
            ClientApplication clientApplication,
            String verifierTransactionId,
            EncryptedAuthorizationResponse encryptedAuthorizationResponse,
            VerificationTransaction verificationTransaction)  {
        if (!StringUtils.hasText(encryptedAuthorizationResponse.getResponse())) {
            throw new VerificationException("invalid_request", "Missing authorization response");
        }
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse(), verificationTransaction);
        String state = (String) claimsFromJwePayload.get("state");
        if (!Objects.equals(state, verificationTransaction.getState())) {
            throw new VerificationException("invalid_request", "Invalid state in authorization response");
        }
        List<DcqlCredentialQuery> requestedCredentials = getRequestedCredentialQueries(verificationTransaction);
        VpToken vpToken = extractVpToken(claimsFromJwePayload);
        auditService.auditReceiveAuthzResponse(vpToken, verifierTransactionId, clientApplication, state);
        Map<String, List<VerifiedCredential>> allCredentials = new LinkedHashMap<>();
        for (DcqlCredentialQuery credentialQuery : requestedCredentials) {
            String credentialId = credentialQuery.getId();
            List<VerifiablePresentation> verifiablePresentations = vpToken.getVerifiablePresentation(credentialId);
            if (verifiablePresentations.isEmpty()) {
                allCredentials.put(credentialId, List.of(new VerifiedCredential(Map.of(), false, List.of())));
                continue;
            }
            if (!credentialQuery.isMultiple() && verifiablePresentations.size() > 1) {
                throw new VerificationException("invalid_request", "Multiple presentations are not allowed for credential id: %s".formatted(credentialId));
            }
            List<VerifiedCredential> parsedCredentials = new ArrayList<>();
            for (VerifiablePresentation verifiablePresentation : verifiablePresentations) {
                parsedCredentials.add(verifiedCredential(verifiablePresentation.value(), verificationTransaction, credentialQuery));
            }
            allCredentials.put(credentialId, parsedCredentials);
        }
        VerifiedCredentials verifiedCredentials = new VerifiedCredentials(allCredentials);
        verificationTransactionService.addVerifiedCredentials(clientApplication, verifierTransactionId, verifiedCredentials, claimsFromJwePayload);
        WalletCallback walletCallback = new WalletCallback();
        if ("same_device".equals(verificationTransaction.getFlow()) && verificationTransaction.getRedirectUri() != null) {
            walletCallback.setRedirectUri(verificationTransaction.getRedirectUri());
        }
        return walletCallback;
    }

    private void markAsError(
            ClientApplication clientApplication,
            String verifierTransactionId,
            Exception responseException) {
        try {
            verificationTransactionService.markAsError(clientApplication, verifierTransactionId);
        } catch (RuntimeException persistenceException) {
            responseException.addSuppressed(persistenceException);
            log.error(
                    "Failed to mark verification transaction {} as error",
                    verifierTransactionId,
                    persistenceException);
        }
    }

    private VpToken extractVpToken(Map<String, Object> claimsFromJwePayload) {
        Object vpTokenObject = claimsFromJwePayload.get("vp_token");
        if (vpTokenObject instanceof Map<?, ?> vpTokenMap) {
            Map<String, List<VerifiablePresentation>> verifiablePresentations = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : vpTokenMap.entrySet()) {
                if (!(entry.getKey() instanceof String credentialId)) {
                    throw new VerificationException("invalid_request", "Unsupported vp_token key type");
                }
                verifiablePresentations.put(credentialId, toVerifiablePresentations(entry.getValue(), credentialId));
            }
            return new VpToken(verifiablePresentations);
        }
        if (vpTokenObject == null) {
            throw new VerificationException("invalid_request", "Missing vp_token");
        }
        throw new VerificationException("invalid_request", "Unsupported vp_token structure");
    }

    private List<ValidationDetail> validationDetails(CredentialVerification verification, ValidationStatus trustlistStatus, ValidationStatus statuslistStatus) {
        List<ValidationDetail> validationDetails = new ArrayList<>();
        validationDetails.add(new ValidationDetail(ValidationType.STATUS_LIST, statuslistStatus, tokenStatuslistService.getValidationDetail(statuslistStatus)));
        validationDetails.add(new ValidationDetail(ValidationType.TRUST_LIST, trustlistStatus, trustlistService.getValidationDetail(trustlistStatus)));
        if (verification.holderBindingStatus() != ValidationStatus.NOT_APPLICABLE) {
            validationDetails.add(new ValidationDetail(ValidationType.HOLDER_BINDING, verification.holderBindingStatus(),
                    detail("Holder binding", "gyldig", "ugyldig", verification.holderBindingStatus())));
        }
        ValidationStatus credentialStatus = verification.credentialStatus();
        validationDetails.add(new ValidationDetail(verification.format().validationType(), credentialStatus, switch (verification.format()) {
            case MSO_MDOC -> detail("mdoc", "mdoc er gyldig", "mdoc er ugyldig", credentialStatus);
            case DC_SD_JWT -> detail("SD-JWT VC", "SDJwt er gyldig", "SDJwt er ugyldig", credentialStatus);
        }));
        return validationDetails;
    }

    private static String detail(String prefix, String valid, String invalid, ValidationStatus status) {
        return prefix + ": " + switch (status) {
            case VALID -> valid;
            case INVALID -> invalid;
            case INCONCLUSIVE -> "validering feila";
            default -> "ukjent status";
        };
    }

    private List<VerifiablePresentation> toVerifiablePresentations(Object vpTokenValue, String credentialId) {
        if (vpTokenValue instanceof List<?> presentations) {
            if (CollectionUtils.isEmpty(presentations)) {
                throw new VerificationException("invalid_request", "Empty presentation array for credential id: %s".formatted(credentialId));
            }
            List<VerifiablePresentation> verifiablePresentations = new ArrayList<>();
            for (Object presentation : presentations) {
                if (!(presentation instanceof String)) {
                    throw new VerificationException("invalid_request", "Unsupported vp_token value type for credential id: " + credentialId);
                }
                verifiablePresentations.add(new VerifiablePresentation((String) presentation));
            }
            return verifiablePresentations;
        }
        if (vpTokenValue instanceof String presentation) {
            return List.of(new VerifiablePresentation(presentation));
        }
        throw new VerificationException("invalid_request", "Unsupported vp_token structure for credential id: " + credentialId);
    }

    private static List<DcqlCredentialQuery> getRequestedCredentialQueries(VerificationTransaction verificationTransaction) {
        if (verificationTransaction.getDcqlQuery() == null || verificationTransaction.getDcqlQuery().getCredentials() == null
                || verificationTransaction.getDcqlQuery().getCredentials().isEmpty()) {
            throw new VerificationException("invalid_request", "Missing credentials in dcql_query");
        }
        for (DcqlCredentialQuery credential : verificationTransaction.getDcqlQuery().getCredentials()) {
            if (credential.getId() == null || credential.getId().isBlank()) {
                throw new VerificationException("invalid_request", "Missing id in dcql_query credential");
            }
        }
        return verificationTransaction.getDcqlQuery().getCredentials();
    }

    private Map<String, Object> decryptAndDeserializeJweResponse(String response, VerificationTransaction verificationTransaction) {
        JWEObject jwe;
        try {
            jwe = JWEObject.parse(response);
        } catch (ParseException e) {
            throw new VerificationException("invalid_request", "Authorization response is not a valid JWE", e);
        }
        validateResponseEncryptionHeader(jwe.getHeader(), verificationTransaction);
        try {
            JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(
                    jwe.getHeader(), verificationTransaction.getEncryptionKey().toECKey().toPrivateKey());
            jwe.decrypt(decrypter);
        } catch (JOSEException e) {
            throw new VerificationException("invalid_request", "Unable to decrypt authorization response", e);
        }
        return jwe.getPayload().toJSONObject();
    }

    private void validateResponseEncryptionHeader(JWEHeader header, VerificationTransaction verificationTransaction) {
        JWK encryptionKey = verificationTransaction.getEncryptionKey();
        if (encryptionKey == null || encryptionKey.getAlgorithm() == null) {
            throw new VerificationException("invalid_request", "Missing response encryption key configuration");
        }
        if (!encryptionKey.getAlgorithm().getName().equals(header.getAlgorithm().getName())) {
            throw new VerificationException("invalid_request", "Authorization response alg does not match the requested encryption key");
        }
        if (encryptionKey.getKeyID() != null && !encryptionKey.getKeyID().equals(header.getKeyID())) {
            throw new VerificationException("invalid_request", "Authorization response kid does not match the requested encryption key");
        }
        if (!ResponseEncryption.SUPPORTED_ENCRYPTION_METHODS.contains(header.getEncryptionMethod())) {
            throw new VerificationException("invalid_request", "Authorization response enc was not advertised in the request");
        }
    }

    /**
     * Resolves the trustlists to check a credential against. PID credentials are issued by a
     * separate set of PID providers, so they are checked against the PID trustlists; all other
     * attestations are checked against the attestation trustlists. The lists are checked in
     * configured priority order and the first match wins.
     */
    protected ValidationStatus checkTrustlist(X509Certificate cert, boolean isPid) {
        List<TrustlistReference> trustlists = isPid
                ? trustlistsProperties.getPidTrustlists()
                : trustlistsProperties.getAttestationTrustlists();
        return trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert, trustlists);
    }

    private ValidationStatus checkStatuslist(StatuslistEntry statuslistEntry, CredentialFormat format) {
        ValidationStatus status;
        if (statuslistEntry != null) {
            int index;
            try {
                index = Integer.parseInt(statuslistEntry.idx());
            } catch (NumberFormatException e) {
                throw new VerificationException("invalid_request", "Invalid status list idx in vp_token");
            }
            if (index < 0) {
                throw new VerificationException("invalid_request", "Invalid status list idx in vp_token");
            }
            status = tokenStatuslistService.lookupStatusFromStatuslist(statuslistEntry.uri(), index, format.statusListTokenFormat());
        } else {
            status = ValidationStatus.NOT_APPLICABLE; // Still valid, but no status in the proof.
        }
        return status;
    }

    private VerifiedCredential verifiedCredential(String vpToken, VerificationTransaction verificationTransaction, DcqlCredentialQuery credentialQuery) {
        CredentialVerifier credentialVerifier = switch (CredentialFormat.fromDcql(credentialQuery.getFormat())) {
            case DC_SD_JWT -> sdJwtCredentialVerifier;
            case MSO_MDOC -> mdocCredentialVerifier;
        };
        CredentialVerification verification = credentialVerifier.verify(vpToken, credentialQuery, verificationTransaction);
        if (verification.format() == CredentialFormat.MSO_MDOC) {
            validateRequestedDocType(verification.credentialType(), credentialQuery);
        }
        ValidationStatus trustlistStatus = verification.issuerCertificate() == null
                ? ValidationStatus.INVALID
                : checkTrustlist(verification.issuerCertificate(), isPid(verification));
        ValidationStatus statuslistStatus = checkStatuslist(verification.statuslistEntry(), verification.format());
        boolean valid = validateStatus(verification.credentialStatus(), statuslistStatus, trustlistStatus, verification.holderBindingStatus());
        return new VerifiedCredential(verification.claims(), valid, verificationTransaction.isIncludeValidationDetails()
                ? validationDetails(verification, trustlistStatus, statuslistStatus)
                : null);
    }

    private void validateRequestedDocType(String docType, DcqlCredentialQuery credentialQuery) {
        DcqlCredentialMeta meta = credentialQuery.getMeta();
        if (meta == null || !StringUtils.hasText(meta.getDoctypeValue())) {
            throw new VerificationException("invalid_request",
                    "Missing or invalid meta.doctype_value for credential id: %s".formatted(credentialQuery.getId()));
        }
        if (docType == null || !meta.getDoctypeValue().equals(docType)) {
            throw new VerificationException("invalid_request",
                    "Returned doctype does not match meta.doctype_value for credential id: %s".formatted(credentialQuery.getId()));
        }
    }

    private static boolean isPid(CredentialVerification verification) {
        String type = verification.credentialType();
        return type != null && switch (verification.format()) {
            case MSO_MDOC -> PID_DOC_TYPE.equals(type);
            case DC_SD_JWT -> PID_VCT_PATTERN.matcher(type).matches();
        };
    }

    private boolean validateStatus(ValidationStatus formatSpecificStatus, ValidationStatus statuslistStatus, ValidationStatus trustlistStatus, ValidationStatus holderBindingStatus) {
        return formatSpecificStatus == ValidationStatus.VALID &&
                (holderBindingStatus == ValidationStatus.VALID || holderBindingStatus == ValidationStatus.NOT_APPLICABLE) &&
                (statuslistStatus == ValidationStatus.VALID || statuslistStatus == ValidationStatus.NOT_APPLICABLE) &&
                trustlistStatus == ValidationStatus.VALID;
    }

}
