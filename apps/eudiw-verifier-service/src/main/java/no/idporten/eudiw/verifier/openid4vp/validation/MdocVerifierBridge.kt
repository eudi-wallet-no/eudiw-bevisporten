package no.idporten.eudiw.verifier.openid4vp
import id.walt.mdoc.verification.VerificationResult;
import id.walt.mdoc.objects.SessionTranscript
import id.walt.mdoc.verification.MdocVerifier
import kotlinx.coroutines.runBlocking

object MdocVerifierBridge {
    fun verifyBlocking(document: id.walt.mdoc.objects.document.Document, sessionTranscript: SessionTranscript): VerificationResult =
        runBlocking {
            MdocVerifier.verify(document, sessionTranscript)
        }
}