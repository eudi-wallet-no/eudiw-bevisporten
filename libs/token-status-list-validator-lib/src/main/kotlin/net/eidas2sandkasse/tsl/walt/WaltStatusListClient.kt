package net.eidas2sandkasse.tsl.walt

import id.walt.certificate.x509.JavaX509CertificateUtil
import id.walt.cose.CoseSign1
import id.walt.cose.coseCompliantCbor
import id.walt.cose.verify
import id.walt.crypto2.CryptoRuntime
import id.walt.crypto2.providers.cryptography.defaultSoftwareKeyProviders
import id.walt.policies2.vc.policies.status.CredentialFetcher
import id.walt.policies2.vc.policies.status.StatusListContent
import id.walt.policies2.vc.policies.status.content.CwtParser
import id.walt.policies2.vc.policies.status.content.JwtParser
import id.walt.policies2.vc.policies.status.model.IETFStatusContent
import id.walt.policies2.vc.policies.status.reader.IETFCwtStatusValueReader
import id.walt.policies2.vc.policies.status.reader.IETFJwtStatusValueReader
import id.walt.policies2.vc.policies.status.reader.format.CwtFormatMatcher
import id.walt.policies2.vc.policies.status.reader.format.JwtFormatMatcher
import id.walt.policies2.vc.policies.status.signature.KeyResolutionFailedException
import id.walt.policies2.vc.policies.status.signature.SignatureInvalidException
import id.walt.policies2.vc.policies.status.signature.StatusListSignatureVerifier
import id.walt.statuslist.codec.TokenStatusListCodec
import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.CborElement
import kotlinx.serialization.cbor.CborInteger
import kotlinx.serialization.cbor.CborMap
import kotlinx.serialization.cbor.CborString
import kotlinx.serialization.cbor.longOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(WaltStatusListClient::class.java)

// Logs every status list actually downloaded. A list served from walt.id's cache is not logged here.
private val StatusListLoadLogging = createClientPlugin("StatusListLoadLogging") {
    onResponse { response ->
        val uri = response.call.request.url
        if (response.status.isSuccess()) {
            log.info("Status list {} loaded. Format: {}. Response: {}.", uri, response.contentType(), response.status)
        } else {
            log.warn("Status list {} could not be loaded. Response: {}.", uri, response.status)
        }
    }
}

/**
 * Blocking bridge to walt.id for the Java code: walt.id's fetcher, verifier and readers are suspend functions
 * returning kotlin.Result, so their names are mangled and cannot be called from Java.
 */
internal class WaltStatusListClient @JvmOverloads constructor(
    connectTimeoutMillis: Long,
    requestTimeoutMillis: Long,
    private val cacheEnabled: Boolean,
    private val clockSkewSeconds: Long,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
    engine: HttpClientEngine = Java.create(),
) {

    private val now: () -> Instant = { Instant.fromEpochMilliseconds(clock.millis()) }

    // KONFIGURASJON: request timeouts. Ktor HttpClient that walt.id's CredentialFetcher downloads with.
    private val httpClient = HttpClient(engine) {
        install(HttpTimeout) {
            this.connectTimeoutMillis = connectTimeoutMillis
            this.requestTimeoutMillis = requestTimeoutMillis
        }
        install(StatusListLoadLogging)
    }

    // KONFIGURASJON: caching. walt.id's CredentialFetcher keeps an in-memory cache of at most
    // CredentialFetcher.MAX_CACHE_ENTRIES (256) lists. Expiry is the earliest of Cache-Control max-age (minus Age),
    // the token's exp and ttl counted from the fetch, and cannot be configured further. Without Cache-Control max-age
    // nothing is cached. Only a single shared instance caches.
    private val sharedFetcher = CredentialFetcher(httpClient, now)

    private val signatureVerifier = StatusListSignatureVerifier()
    private val jwtReader = IETFJwtStatusValueReader(JwtFormatMatcher(), JwtParser())
    private val cwtReader = IETFCwtStatusValueReader(CwtFormatMatcher(), CwtParser())

    fun resolveStatus(uri: String, index: Int): Int = runBlocking {
        val fetcher = if (cacheEnabled) sharedFetcher else CredentialFetcher(httpClient, now)
        val content = fetcher.fetch(uri).getOrThrow()

        val subject: String?
        val iat: Long?
        val exp: Long?
        val statusList: IETFStatusContent
        when (content) {
            is StatusListContent.Text -> {
                val claims = signatureVerifier.verifyJwt(content.content).getOrThrow()
                subject = claims["sub"]?.jsonPrimitive?.contentOrNull
                iat = claims["iat"]?.jsonPrimitive?.longOrNull
                exp = claims["exp"]?.jsonPrimitive?.longOrNull
                statusList = jwtReader.read(content).getOrThrow()
            }

            is StatusListContent.Binary -> {
                val claims = cwtClaims(verifyCwt(content.content))
                subject = (claims[CborInteger(CWT_SUB)] as? CborString)?.value
                iat = (claims[CborInteger(CWT_IAT)] as? CborInteger)?.longOrNull
                exp = (claims[CborInteger(CWT_EXP)] as? CborInteger)?.longOrNull
                statusList = cwtReader.read(content).getOrThrow()
            }
        }

        require(subject == uri) { "Status list sub '$subject' does not match uri '$uri'" }
        checkTokenTimes(uri, iat, exp, now().epochSeconds, clockSkewSeconds)

        val statuses = TokenStatusListCodec.decode(statusList.list)
        val status = TokenStatusListCodec.readStatus(decoded = statuses, index = index, bits = statusList.size).toInt()
        log.debug("Status list {} has status {} at idx {}.", uri, status, index)
        status
    }

    private suspend fun verifyCwt(cwt: ByteArray): ByteArray =
        signatureVerifier.verifyCwt(cwt).recoverCatching { e ->
            if (e !is KeyResolutionFailedException) throw e
            log.debug("walt.id found no key in the protected CWT header, trying x5chain in the unprotected header")
            verifyCwtWithUnprotectedX5Chain(cwt)
        }.getOrThrow()

    // walt.id only resolves x5chain from the protected header, RFC 9360 allows it in the unprotected one too.
    private suspend fun verifyCwtWithUnprotectedX5Chain(cwt: ByteArray): ByteArray {
        val coseSign1 = CoseSign1.fromTagged(cwt)
        val certificate = coseSign1.unprotected?.x5chain?.firstOrNull()
            ?: throw KeyResolutionFailedException("Status list CWT has no x5chain in the protected or unprotected header")
        val key = x509.restoreSubjectPublicKey(x509.parseCertificateDerEncoded(certificate.rawBytes), cryptoRuntime)
        if (!coseSign1.verify(key, ALLOWED_COSE_ALGORITHMS, ByteArray(0))) {
            throw SignatureInvalidException("Status list CWT signature verification failed")
        }
        return coseSign1.payload ?: throw IllegalArgumentException("Status list CWT payload is missing")
    }

    private fun cwtClaims(payload: ByteArray): CborMap =
        coseCompliantCbor.decodeFromByteArray(CborElement.serializer(), payload) as? CborMap
            ?: throw IllegalArgumentException("Status list CWT claims are not a CBOR map")

    companion object {
        private const val CWT_SUB = 2L
        private const val CWT_EXP = 4L
        private const val CWT_IAT = 6L

        // ES256, ES384, ES512, PS256, PS384, PS512
        private val ALLOWED_COSE_ALGORITHMS = setOf(-7, -35, -36, -37, -38, -39)

        private val x509: JavaX509CertificateUtil = JavaX509CertificateUtil.getDefault()
        private val cryptoRuntime = CryptoRuntime(defaultSoftwareKeyProviders(), emptyList())

        /**
         * iat is required (draft-ietf-oauth-status-list-21 §5.1/§5.2), and must not be in the future. exp is
         * optional, but must not have passed (§8.3). walt.id checks neither. Times are epoch seconds; null means
         * missing or unreadable.
         */
        @JvmStatic
        fun checkTokenTimes(uri: String, iat: Long?, exp: Long?, now: Long, clockSkewSeconds: Long) {
            requireNotNull(iat) { "Status list token from '$uri' has no valid iat" }
            require(iat <= now + clockSkewSeconds) { "Status list token from '$uri' has iat in the future" }
            require(exp == null || exp > now - clockSkewSeconds) { "Status list token from '$uri' is expired" }
        }
    }
}
