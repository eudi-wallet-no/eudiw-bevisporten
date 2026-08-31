package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.factories.DefaultJWEDecrypterFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONArrayUtils;
import com.nimbusds.jose.util.X509CertUtils;
import id.walt.mdoc.dataretrieval.DeviceResponse;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.issuersigned.IssuerSigned;
import id.walt.sdjwt.SDJwt;
import id.walt.sdjwt.SimpleJWTCryptoProvider;
import id.walt.sdjwt.VerificationResult;
import id.walt.mdoc.dataelement.*;
import no.idporten.eudiw.verifier.IOConnectionException;
import no.idporten.eudiw.verifier.StatusCommunicationException;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.api.openid4vp.EncryptedAuthorizationResponse;
import no.idporten.eudiw.verifier.api.openid4vp.WalletCallback;
import no.idporten.eudiw.verifier.crypto.ECUtils;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.statuslist.StatusMDoc;
import no.idporten.eudiw.verifier.trustlist.TrustlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import no.idporten.eudiw.verifier.config.ClientApplication;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationDetail;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationType;
import no.idporten.eudiw.verifier.statuslist.StatusSdJwt;
import no.idporten.eudiw.verifier.statuslist.TokenStatuslistService;
import org.jspecify.annotations.NonNull;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OpenID4VPResponseService {

    private static final Logger log = LoggerFactory.getLogger(OpenID4VPResponseService.class);

    private final VerificationTransactionService verificationTransactionService;
    private final TokenStatuslistService tokenStatuslistService;
    private final JsonMapper objectMapper;
    private final TrustlistService trustlistService;

    public OpenID4VPResponseService(VerificationTransactionService verificationTransactionService, TokenStatuslistService tokenStatuslistService, TrustlistService trustlistService, JsonMapper objectMapper) {
        this.verificationTransactionService = verificationTransactionService;
        this.tokenStatuslistService = tokenStatuslistService;
        this.trustlistService = trustlistService;
        this.objectMapper = objectMapper;
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
                    verifiedCredential = handleSDJwt(verifiablePresentation.value(), verificationTransaction.isIncludeValidationDetails());
                } else if ("mso_mdoc".equals(format)) {
                    verifiedCredential = handleMDoc(verifiablePresentation.value(), verificationTransaction.isIncludeValidationDetails());
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

    protected VerifiedCredential handleSDJwt(String vpToken, boolean includeValidationDetails) throws Exception {
        SDJwt unverifiedSDJwt = unverifiedSDJwt(vpToken);
        X509Certificate cert = certificate(unverifiedSDJwt);
        JWSVerifier jwsVerifier = jwsVerifier(cert);
        JWSAlgorithm jwsAlgorithm = algorithm(cert);
        SimpleJWTCryptoProvider cryptoProvider = new SimpleJWTCryptoProvider(jwsAlgorithm, null, jwsVerifier);
        VerificationResult<SDJwt> verificationResult = verificationResult(cryptoProvider, unverifiedSDJwt);
        final Map<String, Object> claims = getClaimsFromSDJwt(verificationResult);
        StatusSdJwt statusRecord = extractStatuslistUriAndIdxSdJwt(verificationResult);
        ValidationStatus status = findStatusFromStatusListSdJwt(statusRecord);
        ValidationStatus trustlistStatus = checkTrustlist(cert);
        List<ValidationDetail> validationDetails = new ArrayList<>();
        if (includeValidationDetails) {
            validationDetails.add(new ValidationDetail(ValidationType.STATUS_LIST, status, getValidationDetail(statusRecord, status)));
            validationDetails.add(new ValidationDetail(ValidationType.TRUST_LIST, trustlistStatus, trustlistService.getValidationDetail(trustlistStatus)));
        }
        ValidationStatus statusForCredential;
        if(status == ValidationStatus.INVALID || trustlistStatus == ValidationStatus.INVALID) {
            statusForCredential = ValidationStatus.INVALID;
        } else if(status == ValidationStatus.INCONCLUSIVE || trustlistStatus == ValidationStatus.INCONCLUSIVE) {
            statusForCredential = ValidationStatus.INCONCLUSIVE;
        } else {
            statusForCredential = ValidationStatus.VALID;
        }

        return new VerifiedCredential(claims, ValidationStatus.VALID == statusForCredential, validationDetails);
    }

    protected VerifiedCredential handleMDoc(String vpToken, boolean includeValidationDetails) {
        DeviceResponse deviceResponse = DeviceResponse.Companion.fromCBORBase64URL(vpToken);
        Map<String, Object> claims = new HashMap<>();
        MDoc mdc = deviceResponse.getDocuments().getFirst();
        verifyMDoc(mdc);
        mDocClaims(mdc.getIssuerSigned(), claims);
        ValidationStatus mdocStatuslist = verificationStatusMdoc(mdc);
        //TODO: trustlist mdoc
        List<ValidationDetail> validationDetails = new ArrayList<>();
        if (includeValidationDetails) {
            //TODO: add validation details both for trustlist and statuslist. Remove hard coded value
            validationDetails.add(new ValidationDetail(ValidationType.STATUS_LIST, mdocStatuslist, "status list check"));
        }
        return new VerifiedCredential(claims, ValidationStatus.VALID == mdocStatuslist, validationDetails);
    }

    protected void verifyMDoc(MDoc mDoc) {
        mDoc.getMSO(); // MSO (Mobile Security Object) verification is not performed here because the issuer's public key or certificate is not available in this context.
        // Proper MSO verification is critical for mdoc validation and should be implemented as soon as the issuer's public key can be obtained.
        // Failing to verify the MSO means the authenticity and integrity of the credential cannot be guaranteed.
        // TODO: Implement MSO verification using the issuer's public key or certificate when it becomes available.
        mDoc.verifyDocType();
        mDoc.verifyIssuerSignedItems();
        mDoc.verifyValidity();
    }


    /**
     * mdoc paths consist of a namespace and an element identifier. The claims are returned as a map of namespace
     * to a map of element identifier to value.
     **/

    protected Map<String, Object> mDocClaims(IssuerSigned issuerSigned, Map<String, Object> claims) {
        for (String namespace : issuerSigned.getNameSpaces().keySet()) {
            List<EncodedCBORElement> elements = issuerSigned.getNameSpaces().get(namespace);
            for (EncodedCBORElement element : elements) {
                Map<MapKey, DataElement> elementMap = ((MapElement) element.decode()).getValue();
                String elementIdentifier = null;
                Object elementValue = null;
                for (MapKey mapKey : elementMap.keySet()) {
                    if (mapKey.getStr().equals("elementIdentifier")) {
                        elementIdentifier = String.valueOf(elementMap.get(mapKey).getInternalValue());
                    }
                    if (mapKey.getStr().equals("elementValue")) {
                        elementValue = extractValue(elementMap.get(mapKey));
                    }
                }
                Map<String, Object> nameSpaceMap = (Map<String, Object>) claims.computeIfAbsent(namespace, _ -> new HashMap<String, Object>());
                nameSpaceMap.put(elementIdentifier, elementValue);
            }
        }
        return claims;
    }

    protected ValidationStatus verificationStatusMdoc(MDoc mDoc) {
        ValidationStatus validationStatus;
        StatusMDoc statusMdoc = extractStatuslistUriAndIdxMDoc(mDoc);
        final int idx;
        try {
            idx = Integer.parseInt(statusMdoc.idx());
        } catch (NumberFormatException e) {
            throw new VerificationException("invalid_request", "Invalid status list idx in vp_token");
        }
        if (statusMdoc.uri() != null && StringUtils.hasText(statusMdoc.uri().toString())) {
           return lookupStatusFromStatuslist(statusMdoc.uri(), idx);
        } else {
            validationStatus = ValidationStatus.VALID;
        }
        return validationStatus;
    }

    protected SDJwt unverifiedSDJwt(String vpToken) {
        return SDJwt.Companion.parse(vpToken);
    }

    protected X509Certificate certificate(SDJwt unverifiedSDJwt) throws Exception{
        JWSHeader jwsHeader = JWSHeader.parse(unverifiedSDJwt.getHeader().toString());
        return X509CertUtils.parse(jwsHeader.getX509CertChain().getFirst().decode());
    }

    protected JWSVerifier jwsVerifier(X509Certificate cert) throws Exception {
        return new ECDSAVerifier((ECPublicKey) cert.getPublicKey());
    }

    protected JWSAlgorithm algorithm(X509Certificate cert) {
        return ECUtils.jwsAlgorithmFromKey(cert.getPublicKey());
    }

    protected VerificationResult<SDJwt> verificationResult(SimpleJWTCryptoProvider jwtCryptoProvider, SDJwt unverifiedSDJwt){
        VerificationResult<SDJwt> verificationResult = unverifiedSDJwt.verify(jwtCryptoProvider, null);
        if (!verificationResult.getVerified()) {
            throw new VerificationException("invalid_request", "Invalid vp_token. Signature verified: %s, disclosures verified: %s".formatted(verificationResult.getSignatureVerified(), verificationResult.getDisclosuresVerified()));
        }
        return verificationResult;
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

    private static @NonNull String getValidationDetail(StatusSdJwt statusRecord, ValidationStatus status) {
        if (statusRecord == null || statusRecord.statuslist() == null) {
            return "Statusliste: beviset er ikkje revokerbart";
        }
        if(status == null || ValidationStatus.INCONCLUSIVE == status) {
            return "Statusliste: validering feila";
        }
        if (ValidationStatus.VALID == status) {
            return "Statusliste: bevis er gyldig";
        }
        if (ValidationStatus.INVALID == status) {
            return "Statusliste: bevis er revokert";
        }
        return "Statusliste: ukjent status";
    }

    private static @NonNull Map<String, Object> getClaimsFromSDJwt(VerificationResult<SDJwt> verificationResult) throws ParseException {
        Map<String, Object> claims = new HashMap<>();
        for (String disclosure : verificationResult.getSdJwt().getDisclosures()) {
            List<Object> parsedDisclosure = JSONArrayUtils.parse(new String(Base64.getUrlDecoder().decode(disclosure)));
            claims.put((String) parsedDisclosure.get(1), parsedDisclosure.get(2));
        }
        return claims;
    }

    private ValidationStatus lookupStatusFromStatuslist(URI uri, int idx) {
        ValidationStatus status;
        try {
            status = tokenStatuslistService.checkStatus(
                    uri,
                    idx,
                    tokenStatuslistService.requestStatusList(uri).getParsedString(),
                    Instant.now());
        } catch (StatusCommunicationException | IOConnectionException e) {
            // TODO: create and update metrics for IOConnectionException.
            status = ValidationStatus.INCONCLUSIVE;
        }
        return status;
    }

    private ValidationStatus findStatusFromStatusListSdJwt(StatusSdJwt statusRecord) {
        ValidationStatus status;
        if (statusRecord != null) {
            status = lookupStatusFromStatuslist(URI.create(statusRecord.statuslist().uri().content()), Integer.parseInt(statusRecord.statuslist().idx().content()));
        } else {
            status = ValidationStatus.VALID;
        }
        return status;
    }

    protected StatusSdJwt extractStatuslistUriAndIdxSdJwt(VerificationResult<SDJwt> sdjwt) {
        Object statusObj = sdjwt.getSdJwt().getFullPayload().get("status");
        if (Objects.isNull(statusObj) || !StringUtils.hasText(statusObj.toString())) {
            return null;
        }
        return objectMapper.convertValue(statusObj, StatusSdJwt.class);
    }

    protected StatusMDoc extractStatuslistUriAndIdxMDoc(MDoc mDoc) {
        if(!Objects.isNull(mDoc.getMSO()) && !Objects.isNull(mDoc.getMSO().getStatus()) && !Objects.isNull(mDoc.getMSO().getStatus().getStatusList())) {
         return new StatusMDoc(mDoc.getMSO().getStatus().getStatusList().toJSON().get("idx").toString(), URI.create(mDoc.getMSO().getStatus().getStatusList().getUri()));
    }
        return null;
    }

    protected Object extractValue(DataElement dataElement) {
        if (dataElement == null) {
            return null;
        }
        if (dataElement instanceof BooleanElement) {
            return ((BooleanElement) dataElement).getValue();
        }
        return switch (dataElement.getType()) {
            case number -> ((NumberElement) dataElement).getValue();
            case textString -> ((StringElement) dataElement).getValue();
            case dateTime -> ((DateTimeElement) dataElement).getValue().toString();
            case fullDate -> ((FullDateElement) dataElement).getValue().toString();
            case nil -> null;
            case map -> ((MapElement) dataElement).getValue().entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            e -> extractValue(e.getValue())));
            case list -> ((ListElement) dataElement).getValue()
                    .stream()
                    .map(this::extractValue)
                    .toList();
            case byteString -> new String(Base64.getEncoder().encode(((ByteStringElement) dataElement).getValue()));
            default -> String.valueOf(dataElement.getInternalValue());
        };

    }


}
