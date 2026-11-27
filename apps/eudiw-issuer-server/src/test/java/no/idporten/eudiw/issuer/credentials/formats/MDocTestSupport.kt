package no.idporten.eudiw.issuer.credentials.formats

import id.walt.cose.CoseVerifier
import id.walt.cose.coseCompliantCbor
import id.walt.mdoc.objects.document.IssuerSigned
import id.walt.mdoc.objects.elements.IssuerSignedItem
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.cbor.*
import java.security.Signature
import java.security.cert.X509Certificate

/** Reads walt.id mdoc-credentials2 types from Java tests. */
object MDocTestSupport {

    @JvmStatic
    fun items(issuerSigned: IssuerSigned, namespace: String): List<IssuerSignedItem> =
        issuerSigned.namespaces?.get(namespace)?.entries?.map { it.value } ?: emptyList()

    /** The element value as a plain Java value; tagged values become "tag:value". */
    @JvmStatic
    fun value(item: IssuerSignedItem): Any? = plain(item.elementValue)

    @JvmStatic
    fun decode(bytes: ByteArray): IssuerSigned =
        coseCompliantCbor.decodeFromByteArray(IssuerSigned.serializer(), bytes)

    @JvmStatic
    fun verifySignature(issuerSigned: IssuerSigned, certificate: X509Certificate): Boolean = runBlocking {
        issuerSigned.issuerAuth.verify(CoseVerifier { data, signature ->
            Signature.getInstance("SHA256withECDSAinP1363Format").run {
                initVerify(certificate.publicKey)
                update(data)
                verify(signature)
            }
        })
    }

    private fun plain(element: CborElement): Any? {
        val value: Any? = when (element) {
            is CborString -> element.value
            is CborBoolean -> element.value
            is CborInteger -> element.long
            is CborByteString -> element.toByteArray()
            is CborArray -> element.map { plain(it) }
            is CborMap -> element.entries.associate { (k, v) -> plain(k) to plain(v) }
            else -> element
        }
        return if (element.tags.isEmpty()) value else "${element.tags.joinToString(":")}:$value"
    }
}
