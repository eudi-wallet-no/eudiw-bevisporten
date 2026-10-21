package no.idporten.eudiw.verifier.openid4vp;

import id.walt.mdoc.objects.MdocsCborSerializer;
import id.walt.mdoc.objects.MdocsCborSerializerKt;
import id.walt.mdoc.objects.SessionTranscript;
import id.walt.mdoc.objects.deviceretrieval.DeviceResponse;
import id.walt.mdoc.objects.document.Document;
import id.walt.mdoc.parser.MdocParser;
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

import java.nio.charset.StandardCharsets;
import java.util.*;


@Service
public class MDocService  {

    private static final Logger log = LogManager.getLogger(MDocService.class);

    public MDocService() {
    }

    protected VerificationResult verifyMDoc(String vpToken, VerificationTransaction verificationTransaction) {
        try {
            Document doc = MdocParser.INSTANCE.parseToDocument(vpToken);

            MdocVerificationContext context = new MdocVerificationContext(
                    verificationTransaction.getNonce(),
                    verificationTransaction.getAudience(),
                    verificationTransaction.getRedirectUri() == null ? "" : verificationTransaction.getRedirectUri().toString(),
                    null,
                    false,
                    false,
                    false,
                    null
            );

            SessionTranscript transcript = MdocVerifier.INSTANCE.buildSessionTranscriptForContext(context);
            return MdocVerifierBridge.INSTANCE.verifyBlocking(doc, transcript);

        } catch (Exception e) {
            log.error("Failed to parse/verify mdoc", e);
            throw new VerificationException("invalid_request", "Failed to parse mdoc", e);
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
