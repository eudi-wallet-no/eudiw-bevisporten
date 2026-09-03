package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.JWK;
import id.walt.mdoc.doc.MDoc;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.api.openid4vp.WalletCallback;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationDetail;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationType;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;

import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.*;

@Component
public class OpenID4VPResponseService {

    private static final Logger log = LoggerFactory.getLogger(OpenID4VPResponseService.class);

    private final VerificationTransactionService verificationTransactionService;
    private final TokenStatuslistService tokenStatuslistService;
    private final TrustlistService trustlistService;

    private final MDocService mDocService;

    private final SdJwtService sdJwtService;

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService, TokenStatuslistService tokenStatuslistService, TrustlistService trustlistService, MDocService mDocService, SdJwtService sdJwtService) {
        this.verificationTransactionService = verificationTransactionService;
        this.tokenStatuslistService = tokenStatuslistService;
        this.trustlistService = trustlistService;
        this.mDocService = mDocService;
        this.sdJwtService = sdJwtService;
    }

    public WalletCallback receiveResponse(ClientApplication clientApplication, String verifierTransactionId, EncryptedAuthorizationResponse encryptedAuthorizationResponse) throws Exception {
        VerificationTransaction verificationTransaction = verificationTransactionService.getVerificationTransaction(clientApplication, verifierTransactionId);
        if (verificationTransaction == null) {
            throw new VerificationException("invalid_request", "Unknown verification transaction id");
        }
        Map<String, Object> claimsFromJwePayload = decryptAndDeserializeJweResponse(encryptedAuthorizationResponse.getResponse(), verificationTransaction.getEncryptionKey());
        String nonce = (String) claimsFromJwePayload.get("nonce");
        String state = (String) claimsFromJwePayload.get("state");
        if (!Objects.equals(state, verificationTransaction.getState())) {
            throw new VerificationException("invalid_request", "Invalid state in authorization response");
        }
        List<DcqlCredentialQuery> requestedCredentials = getRequestedCredentialQueries(verificationTransaction);
        VpToken vpToken = extractVpToken(claimsFromJwePayload);
        Map<String, List<VerifiedCredential>> allCredentials = new LinkedHashMap<>();
        for (DcqlCredentialQuery credentialQuery : requestedCredentials) {
            String credentialId = credentialQuery.getId();
            List<VerifiablePresentation> verifiablePresentations = vpToken.getVerifiablePresentation(credentialId);
            if (verifiablePresentations.isEmpty()) {
                allCredentials.put(credentialId, List.of(new VerifiedCredential(Map.of(), false, List.of())));
                continue;
            }
            String format = credentialQuery.getFormat();
            List<VerifiedCredential> parsedCredentials = new ArrayList<>();
            for (VerifiablePresentation verifiablePresentation : verifiablePresentations) {
                VerifiedCredential verifiedCredential;
                if ("dc+sd-jwt".equals(format)) {
                    verifiedCredential = sdJwtVerifiedCredential(verifiablePresentation.value(), verificationTransaction);
                } else if ("mso_mdoc".equals(format)) {
                    verifiedCredential = mdocVerifiedCredential(verifiablePresentation.value(), verificationTransaction);
                } else {
                    throw new VerificationException("invalid_request", "Unsupported credential format: " + format);
                }
                parsedCredentials.add(verifiedCredential);
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
        return new VpToken(Map.of());
    }

    private List<ValidationDetail> validationDetails(boolean isIncludeValidationStatus, ValidationStatus trustlistStatus, ValidationStatus statuslistStatus, ValidationStatus formatSpecificStatus, String format) {
        if (!isIncludeValidationStatus) {
            return null;
        }
        List<ValidationDetail> validationDetails = new ArrayList<>();
        validationDetails.add(new ValidationDetail(ValidationType.STATUS_LIST, statuslistStatus, tokenStatuslistService.getValidationDetail(statuslistStatus)));
        validationDetails.add(new ValidationDetail(ValidationType.TRUST_LIST, trustlistStatus, trustlistService.getValidationDetail(trustlistStatus)));
        if(format.equals("MDoc")) {
            validationDetails.add(new ValidationDetail(ValidationType.MDOC, formatSpecificStatus, mDocService.getValidationDetail(formatSpecificStatus)));
        } else if(format.equals("SDJwt")) {
            validationDetails.add(new ValidationDetail(ValidationType.SDJWT, formatSpecificStatus, sdJwtService.getValidationDetail(formatSpecificStatus)));
        }
        return validationDetails;

    }

    private List<VerifiablePresentation> toVerifiablePresentations(Object vpTokenValue, String credentialId) {
        if (vpTokenValue instanceof List<?> presentations) {
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

    private Map<String, Object> decryptAndDeserializeJweResponse(String response, JWK encryptionKey) throws ParseException, JOSEException {
        JWEObject jwe = JWEObject.parse(response);
        JWEDecrypter decrypter = new DefaultJWEDecrypterFactory().createJWEDecrypter(jwe.getHeader(), encryptionKey.toECKey().toPrivateKey());
        jwe.decrypt(decrypter);
        return jwe.getPayload().toJSONObject();
    }

    protected ValidationStatus checkTrustlist(X509Certificate cert) {
        return trustlistService.checkIfCertificateFromJwsHeaderIsOnTrustlist(cert);
    }


    private ValidationStatus checkStatuslist(StatuslistEntry statuslistEntry) {
        ValidationStatus status;
        if (statuslistEntry != null) {
            int index;
            try {
                index = Integer.parseInt(statuslistEntry.idx());
            } catch (NumberFormatException e) {
                throw new VerificationException("invalid_request", "Invalid status list idx in vp_token");
            }
            status = tokenStatuslistService.lookupStatusFromStatuslist(statuslistEntry.uri(), index);
        } else {
            status = ValidationStatus.NON_VERIFIABLE; // Still valid, but no status in the proof.
        }
        return status;
    }

    private VerifiedCredential sdJwtVerifiedCredential(String vpToken, VerificationTransaction verificationTransaction) {
        SDJwt unverifiedSDJwt = sdJwtService.sdJwtFromVpToken(vpToken);
        X509Certificate cert = sdJwtService.certificate(unverifiedSDJwt);
        VerificationResult<SDJwt> verificationResult = sdJwtService.verifySdJwt(unverifiedSDJwt, cert);
        ValidationStatus sdJwtStatus = sdJwtService.validationStatusSdJwt(verificationResult);
        Map<String, Object> claims = sdJwtService.sdJwtClaims(verificationResult);

        StatuslistEntry statuslistRecord = sdJwtService.extractStatuslistUriAndIdx(verificationResult);
        ValidationStatus statuslistStatus = checkStatuslist(statuslistRecord);
        ValidationStatus trustlistStatus = checkTrustlist(sdJwtService.certificate(verificationResult.getSdJwt()));
        if (sdJwtStatus == ValidationStatus.VALID && statuslistStatus == ValidationStatus.VALID && trustlistStatus == ValidationStatus.VALID) {
            return new VerifiedCredential(claims, true, validationDetails(verificationTransaction.isIncludeValidationDetails(), trustlistStatus, statuslistStatus, sdJwtStatus, "SDJwt"));
        }
        else {
            return new VerifiedCredential(claims, false, validationDetails(verificationTransaction.isIncludeValidationDetails(), trustlistStatus, statuslistStatus, sdJwtStatus, "SDJwt"));
        }
    }

    private VerifiedCredential mdocVerifiedCredential(String vpToken, VerificationTransaction verificationTransaction) {
        MDoc mdoc = mDocService.mDocFromVpToken(vpToken);
        Map<String, Object> claims = mDocService.claimsFromMDoc(mdoc);
        ValidationStatus mdocStatus = mDocService.verifyMDoc(mdoc);
        ValidationStatus trustlistStatus = checkTrustlist(mDocService.extractCertificateFromMdoc(mdoc));
        ValidationStatus statuslistStatus = checkStatuslist(mDocService.extractStatuslistUriAndIdx(mdoc));
        if(mdocStatus == ValidationStatus.VALID && trustlistStatus == ValidationStatus.VALID && statuslistStatus == ValidationStatus.VALID) {
            return new VerifiedCredential(claims, true, validationDetails(verificationTransaction.isIncludeValidationDetails(),trustlistStatus, statuslistStatus, mdocStatus, "MDoc"));
        } else {
            return new VerifiedCredential(claims, false, validationDetails(verificationTransaction.isIncludeValidationDetails(),trustlistStatus, statuslistStatus, mdocStatus, "MDoc"));
        }

    }

}
