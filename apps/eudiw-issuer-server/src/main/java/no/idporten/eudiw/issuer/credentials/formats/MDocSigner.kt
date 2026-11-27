package no.idporten.eudiw.issuer.credentials.formats

import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import id.walt.cose.Cose
import id.walt.cose.CoseCertificate
import id.walt.cose.CoseKey
import id.walt.cose.coseCompliantCbor
import id.walt.crypto2.algorithms.DigestAlgorithm
import id.walt.crypto2.algorithms.EcdsaSignatureEncoding
import id.walt.crypto2.algorithms.SignatureAlgorithm
import id.walt.crypto2.keys.EcCurve
import id.walt.crypto2.keys.KeyCapabilities
import id.walt.crypto2.keys.KeyId
import id.walt.crypto2.keys.KeySpec
import id.walt.crypto2.keys.KeyUsage
import id.walt.crypto2.keys.SigningKey
import id.walt.mdoc.encoding.toMdocTDateString
import id.walt.mdoc.issuance.MdocIssuer
import id.walt.mdoc.objects.document.IssuerSigned
import id.walt.mdoc.objects.elements.IssuerSignedItem
import id.walt.mdoc.objects.mso.Status
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.CborArray
import kotlinx.serialization.cbor.CborBoolean
import kotlinx.serialization.cbor.CborByteString
import kotlinx.serialization.cbor.CborElement
import kotlinx.serialization.cbor.CborInteger
import kotlinx.serialization.cbor.CborMap
import kotlinx.serialization.cbor.CborString
import no.idporten.eudiw.issuer.credentials.types.*
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.Base64
import kotlin.time.Instant

/**
 * Wapper around walt.id mdoc-credentials2 for signing mdoc credentials.
 */
object MDocSigner {

    private const val TAG_TDATE = 0uL
    private const val TAG_FULL_DATE = 1004uL

    /**
     * Sign an mdoc.
     */
    @JvmStatic
    fun sign(
        docType: String,
        namespaces: Map<String, List<IssuerSignedItem>>,
        issuerKey: PrivateKey,
        issuerCertificates: List<X509Certificate>,
        deviceKey: ECKey,
        signedAt: java.time.Instant,
        validUntil: java.time.Instant,
        status: Status?,
    ): IssuerSigned = runBlocking {
        MdocIssuer.signMsoForIssuerSignedObjects(
            namespaceIssuerSignedItems = namespaces,
            issuerKey = JcaP256SigningKey(issuerKey),
            signatureAlgorithm = Cose.Algorithm.ES256,
            issuerCertificate = issuerCertificates.map { CoseCertificate(it.encoded) },
            holderKey = toCoseKey(deviceKey),
            docType = docType,
            signedAt = signedAt.toKotlinInstant(),
            validFrom = signedAt.toKotlinInstant(),
            validUntil = validUntil.toKotlinInstant(),
            expectedUpdate = validUntil.toKotlinInstant(),
            status = status,
        )
    }

    /**
     * Encode to byte array.
     */
    @JvmStatic
    fun encode(issuerSigned: IssuerSigned): ByteArray =
        coseCompliantCbor.encodeToByteArray(IssuerSigned.serializer(), issuerSigned)

    /**
     * Create an issuer signed item for a claim.
     */
    @JvmStatic
    fun issuerSignedItem(digestId: Int, elementIdentifier: String, value: ClaimValue): IssuerSignedItem =
        IssuerSignedItem.create(digestId.toUInt(), elementIdentifier, toCbor(value))

    private fun toCbor(claimValue: ClaimValue?): CborElement = when (claimValue) {
        is StringValue -> CborString(claimValue.value)
        is BooleanValue -> CborBoolean(claimValue.value)
        is NumberValue -> CborInteger(claimValue.value)
        // mimeType is only for SD-JWT
        is BinaryValue -> CborByteString(Base64.getDecoder().decode(claimValue.value))
        is DateTimeValue -> CborString(
            Instant.fromEpochSeconds(claimValue.value.toEpochSecond()).toMdocTDateString(),
            TAG_TDATE
        )
        is FullDateValue -> CborString(claimValue.value.toString(), TAG_FULL_DATE)
        is ListValue -> CborArray(claimValue.value.map { toCbor(it) })
        is MapValue -> CborMap(claimValue.value.entries.associate { (key, value) ->
            require(value is StringValue || value is FullDateValue) { "Unsupported map value type: ${value.javaClass}" }
            CborString(key) to toCbor(value)
        })
        else -> throw IllegalArgumentException("Unsupported data element type: $claimValue")
    }

    private fun toCoseKey(jwk: ECKey): CoseKey = CoseKey(
        kty = Cose.KeyTypes.EC2,
        crv = when (jwk.curve) {
            Curve.P_256 -> Cose.EllipticCurves.P_256
            Curve.P_384 -> Cose.EllipticCurves.P_384
            Curve.P_521 -> Cose.EllipticCurves.P_521
            else -> throw IllegalArgumentException("Unsupported device key curve: ${jwk.curve}")
        },
        x = jwk.x.decode(),
        y = jwk.y.decode(),
    )

    private fun java.time.Instant.toKotlinInstant(): Instant = Instant.fromEpochSeconds(epochSecond)

    /** Signs with the keystore's JCA key, so the private key never has to be exported. */
    private class JcaP256SigningKey(private val privateKey: PrivateKey) : SigningKey {
        private val es256 = SignatureAlgorithm.Ecdsa(DigestAlgorithm.SHA_256, EcdsaSignatureEncoding.IEEE_P1363)

        override val id = KeyId("issuer")
        override val spec = KeySpec.Ec(EcCurve.P256)
        override val usages = setOf(KeyUsage.SIGN)
        override val capabilities = KeyCapabilities(signer = this, signatureAlgorithms = setOf(es256))

        override suspend fun sign(data: ByteArray, algorithm: SignatureAlgorithm): ByteArray {
            require(algorithm == es256) { "Unsupported signature algorithm: $algorithm" }
            return Signature.getInstance("SHA256withECDSAinP1363Format").run {
                initSign(privateKey)
                update(data)
                sign()
            }
        }
    }
}
