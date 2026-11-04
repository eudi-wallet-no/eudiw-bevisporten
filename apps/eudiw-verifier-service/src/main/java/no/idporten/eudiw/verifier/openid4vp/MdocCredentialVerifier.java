package no.idporten.eudiw.verifier.openid4vp;

import com.nimbusds.jose.util.X509CertUtils;
import id.walt.cose.CoseInternalKt;
import id.walt.credentials.presentations.formats.MsoMdocPresentation;
import id.walt.mdoc.encoding.ByteStringWrapper;
import id.walt.mdoc.objects.deviceretrieval.DeviceResponse;
import id.walt.mdoc.objects.document.Document;
import id.walt.mdoc.objects.elements.IssuerSignedItem;
import id.walt.mdoc.objects.elements.IssuerSignedList;
import id.walt.mdoc.objects.mso.MobileSecurityObject;
import id.walt.mdoc.objects.mso.Status;
import id.walt.policies2.vp.policies.*;
import id.walt.policies2.vp.policies.VPPolicy2.PolicyRunResult;
import kotlinx.serialization.cbor.*;
import kotlinx.serialization.json.Json;
import kotlinx.serialization.json.JsonArray;
import kotlinx.serialization.json.JsonElementKt;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.dcql.DcqlCredentialQuery;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import no.idporten.eudiw.verifier.statuslist.StatuslistEntry;
import no.idporten.eudiw.verifier.walt.WaltPresentations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Verifies mdoc presentations (a DeviceResponse). Holder binding is verified with device authentication over the
 * SessionTranscript. Only one document per DeviceResponse is accepted (HAIP), and its doctype must match the MSO.
 */
@Service
public class MdocCredentialVerifier extends CredentialVerifier {

    private static final Logger log = LoggerFactory.getLogger(MdocCredentialVerifier.class);

    /** Policy that verifies device authentication over the SessionTranscript, i.e. holder binding. */
    static final String DEVICE_AUTH_POLICY = "mso_mdoc/device-auth";

    private static final Predicate<String> DEVICE_AUTH = DEVICE_AUTH_POLICY::equals;

    private final List<MdocVPPolicy> policies;

    @Autowired
    public MdocCredentialVerifier() {
        this(Clock.systemUTC(), defaultPolicies());
    }

    MdocCredentialVerifier(Clock clock, List<MdocVPPolicy> policies) {
        super(clock);
        this.policies = policies;
    }

    static List<MdocVPPolicy> defaultPolicies() {
        return List.of(
                new DeviceAuthMdocVpPolicy(),
                new IssuerAuthMdocVpPolicy(),
                new IssuerSignedDataMdocVpPolicy(),
                new MsoVerificationMdocVpPolicy());
    }

    /**
     * Holder binding is verified with device authentication when the DCQL query requires it, otherwise the device
     * signature is not checked and holder binding is not applicable.
     */
    @Override
    public CredentialVerification verify(String vpToken, DcqlCredentialQuery credentialQuery, VerificationTransaction transaction) {
        boolean holderBindingRequired = credentialQuery.getRequireCryptographicHolderBinding();
        requireSingleDocument(vpToken);
        MsoMdocPresentation presentation = parse(() -> WaltPresentations.parseMdocPresentation(vpToken), "Failed to parse mdoc vp_token");
        Document document = presentation.getMdoc().getDocument();

        List<MdocVPPolicy> selectedPolicies = holderBindingRequired ? policies : without(policies, DEVICE_AUTH);
        Map<String, PolicyRunResult> results = runPolicies(presentation,
                new VPPolicyList(List.of(), List.of(), selectedPolicies), vpToken, transaction);

        ValidationStatus holderBindingStatus = ValidationStatus.NOT_APPLICABLE;
        if (holderBindingRequired) {
            // A policy list without device authentication must not make holder binding pass
            holderBindingStatus = results.containsKey(DEVICE_AUTH_POLICY) ? status(results, DEVICE_AUTH) : ValidationStatus.INVALID;
            if (holderBindingStatus != ValidationStatus.VALID) {
                throw holderBindingFailed();
            }
        }
        ValidationStatus credentialStatus = status(results, DEVICE_AUTH.negate());

        MobileSecurityObject mso = mso(document);
        if (mso != null && !document.getDocType().equals(mso.getDocType())) {
            log.warn("mdoc doctype {} does not match MSO doctype {}", document.getDocType(), mso.getDocType());
            credentialStatus = ValidationStatus.INVALID;
        }
        return new CredentialVerification(
                CredentialFormat.MSO_MDOC,
                document.getDocType(),
                claims(document),
                issuerCertificate(results.get("mso_mdoc/issuer_auth")),
                mso == null ? null : statuslistEntry(mso),
                mso == null ? ValidationStatus.INVALID : credentialStatus,
                holderBindingStatus);
    }

    /**
     * According to HAIP §5.3.1, only one document per DeviceResponse is supported. walt.id silently uses the first.
     * https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html#name-openid-for-verifiable-crede
     */
    static void requireSingleDocument(String vpToken) {
        DeviceResponse deviceResponse = parse(() -> CoseInternalKt.getCoseCompliantCbor().decodeFromByteArray(
                DeviceResponse.Companion.serializer(), Base64.getUrlDecoder().decode(vpToken)),
                "Failed to parse mdoc vp_token");
        if (deviceResponse.getDocuments() == null || deviceResponse.getDocuments().length == 0) {
            throw new VerificationException("invalid_request", "No mdoc documents in vp_token");
        }
        if (deviceResponse.getDocuments().length > 1) {
            throw new VerificationException("invalid_request", "Multiple mdoc documents in vp_token, only one is supported");
        }
    }

    // The document signer certificate issuerAuth was verified with, null if issuer authentication failed
    private static X509Certificate issuerCertificate(PolicyRunResult issuerAuth) {
        if (issuerAuth == null || !(issuerAuth.getResults().get("certificate_chain") instanceof JsonArray chain) || chain.isEmpty()) {
            return null;
        }
        String leaf = ((JsonPrimitive) chain.getFirst()).getContent();
        return X509CertUtils.parse(Base64.getDecoder().decode(leaf));
    }

    // Claims by namespace and element identifier
    private static Map<String, Object> claims(Document document) {
        Map<String, Object> claims = new HashMap<>();
        if (document.getIssuerSigned().getNamespaces() == null) {
            return claims;
        }
        for (Map.Entry<String, IssuerSignedList> namespace : document.getIssuerSigned().getNamespaces().entrySet()) {
            Map<String, Object> elements = new HashMap<>();
            for (ByteStringWrapper<IssuerSignedItem> wrappedItem : namespace.getValue().getEntries()) {
                IssuerSignedItem item = wrappedItem.getValue();
                elements.put(item.getElementIdentifier(), cborValue(item.getElementValue()));
            }
            claims.put(namespace.getKey(), elements);
        }
        return claims;
    }

    static Object cborValue(CborElement element) {
        return switch (element) {
            case null -> null;
            case CborBoolean booleanElement -> booleanElement.getValue();
            case CborInteger integerElement -> CborElementKt.getLong(integerElement);
            case CborFloat floatElement -> floatElement.getValue();
            // tdate (tag 0) and full-date (tag 1004) are text strings
            case CborString stringElement -> stringElement.getValue();
            case CborNull _, CborUndefined _ -> null;
            case CborMap mapElement -> mapElement.entrySet().stream().collect(Collectors.toMap(
                    e -> String.valueOf(cborValue(e.getKey())),
                    e -> cborValue(e.getValue())));
            case CborArray listElement -> listElement.stream().map(MdocCredentialVerifier::cborValue).toList();
            case CborByteString byteStringElement -> Base64.getEncoder().encodeToString(byteStringElement.getBytes());
            default -> String.valueOf(element);
        };
    }

    private static StatuslistEntry statuslistEntry(MobileSecurityObject mso) {
        Status status = mso.getStatus();
        if (status == null || status.getStatusList() == null) {
            return null;
        }
        // idx is an unsigned long and uri a value class, neither has a Java accessible getter
        JsonObject statusList = JsonElementKt.getJsonObject(Json.Default.encodeToJsonElement(
                Status.StatusListInfo.Companion.serializer(), status.getStatusList()));
        return new StatuslistEntry(
                JsonElementKt.getJsonPrimitive(statusList.get("idx")).getContent(),
                URI.create(JsonElementKt.getJsonPrimitive(statusList.get("uri")).getContent()));
    }

    // null when the MSO cannot be decoded, e.g. with fractions of seconds in validityInfo (ISO/IEC 18013-5)
    private static MobileSecurityObject mso(Document document) {
        try {
            return document.getIssuerSigned().decodeMobileSecurityObject();
        } catch (IllegalArgumentException e) {
            log.warn("Unable to parse MSO of mdoc: {}", e.getMessage());
            return null;
        }
    }
}
