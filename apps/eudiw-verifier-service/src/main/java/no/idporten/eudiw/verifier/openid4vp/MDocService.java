package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.objects.SessionTranscript;
import id.walt.mdoc.objects.deviceretrieval.DeviceResponse;
import id.walt.mdoc.objects.document.Document;
import kotlinx.serialization.cbor.Cbor;
import id.walt.mdoc.verification.MdocVerifier;
import id.walt.mdoc.verification.MdocVerificationContext;
import id.walt.mdoc.verification.VerificationResult;
import no.idporten.eudiw.verifier.VerificationException;
import no.idporten.eudiw.verifier.openid4vp.validation.ValidationStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class MDocService  {

    private static final Logger log = LogManager.getLogger(MDocService.class);

    public MDocService() {
    }

    protected VerificationResult verifyMDoc(String vpToken, VerificationTransaction verificationTransaction) {
        // Parse the string into a Document object
        log.info("Parsing mdoc from vp_token..." + vpToken);
        Optional<Document> document;
        try {
            byte[] cborBytes = Base64.getUrlDecoder().decode(vpToken);
            DeviceResponse response = Cbor.Default.decodeFromByteArray(DeviceResponse.Companion.serializer(), cborBytes);
//            response = MdocParser.INSTANCE.parseToDocument(Arrays.toString(cborBytes));
            document = Arrays.stream(response.getDocuments()).findFirst();

        } catch (Exception e) {
            log.error("Failed to parse mdoc: " + e.getMessage(), e);
            throw new VerificationException("invalid_request", "Failed to parse mdoc", e);
        }

        try {
            MdocVerificationContext context = new MdocVerificationContext(
                    verificationTransaction.getNonce(),
                    verificationTransaction.getAudience(),
                    verificationTransaction.getRedirectUri().toString(),
                    null,
                    false,
                    false,
                    false,
                    null
            );
            SessionTranscript sessionTranscript = MdocVerifier.INSTANCE.buildSessionTranscriptForContext(context);

            return MdocVerifierBridge.INSTANCE.verifyBlocking(document.get(), sessionTranscript);
        }
        catch (Exception e) {
            log.warn("Mdoc verification failed via MdocVerifier", e);
            throw new VerificationException("invalid_request", "Mdoc verification failed", e);
        }
    }

    public @NonNull String getValidationDetail(ValidationStatus status) {
        switch (status) {
            case INCONCLUSIVE:
                return "mdoc: validering feila";
            case VALID:
                return "mdoc: mdoc er gyldig";
            case INVALID:
                return "mdoc: mdoc er ugyldig";
            default:
                return "mdoc: ukjent status";
        }
    }
}
