package no.idporten.eudiw.verifier.walt

import id.walt.credentials.CredentialParser
import id.walt.credentials.formats.DigitalCredential
import id.walt.credentials.presentations.formats.DcSdJwtPresentation
import id.walt.credentials.presentations.formats.MsoMdocPresentation
import id.walt.credentials.presentations.formats.VerifiablePresentation
import id.walt.policies2.vp.policies.VPPolicy2.PolicyRunResult
import id.walt.policies2.vp.policies.VPPolicyList
import id.walt.policies2.vp.policies.VPPolicyRunner
import id.walt.policies2.vp.policies.VerificationSessionContext
import id.walt.verifier.openid.models.openid.OpenID4VPResponseMode
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant

/**
 * Blocking entry points to walt.id for the Java code. The walt.id parsers and policy runner are suspend functions,
 * and the parsers return kotlin.Result, so their names are mangled and they cannot be called from Java.
 */
object WaltPresentations {

    /** SD-JWT VC presentation with key binding JWT (issuer-signed JWT~disclosures~KB-JWT). */
    @JvmStatic
    fun parseSdJwtPresentation(vpToken: String): DcSdJwtPresentation =
        runBlocking { DcSdJwtPresentation.parse(vpToken).getOrThrow() }

    /** mdoc presentation, a base64url encoded DeviceResponse. */
    @JvmStatic
    fun parseMdocPresentation(vpToken: String): MsoMdocPresentation =
        runBlocking { MsoMdocPresentation.parse(vpToken).getOrThrow() }

    /** Any credential, the format is detected from the content. */
    @JvmStatic
    fun parseCredential(credential: String): DigitalCredential =
        runBlocking { CredentialParser.detectAndParse(credential).second }

    @JvmStatic
    fun verify(
        presentation: VerifiablePresentation,
        policies: VPPolicyList,
        context: VerificationSessionContext,
    ): Map<String, PolicyRunResult> =
        runBlocking { VPPolicyRunner.verifyPresentation(presentation, policies, context) }

    /**
     * Session data from the authorization request, for an encrypted direct_post.jwt response. client_id (audience),
     * nonce, response_uri and the thumbprint of the response encryption key make up the mdoc SessionTranscript
     * (OpenID4VPHandover, OpenID4VP 1.0 appendix B.2.6.1).
     */
    @JvmStatic
    fun sessionContext(
        vpToken: String,
        nonce: String?,
        audience: String?,
        responseUri: String?,
        jwkThumbprint: String?,
        verificationTime: java.time.Instant,
    ): VerificationSessionContext = VerificationSessionContext(
        vpToken = vpToken,
        expectedNonce = nonce.orEmpty(),
        expectedAudience = audience,
        expectedOrigins = null,
        responseUri = responseUri,
        responseMode = OpenID4VPResponseMode.DIRECT_POST_JWT,
        isSigned = true,
        isEncrypted = true,
        jwkThumbprint = jwkThumbprint,
        isAnnexC = false,
        verificationTime = Instant.fromEpochSeconds(verificationTime.epochSecond, verificationTime.nano),
    )
}
