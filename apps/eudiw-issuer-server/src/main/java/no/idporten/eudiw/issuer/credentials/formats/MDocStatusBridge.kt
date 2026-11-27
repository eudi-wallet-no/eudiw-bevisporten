package no.idporten.eudiw.issuer.credentials.formats

import id.walt.mdoc.objects.mso.Status
import id.walt.mdoc.objects.mso.UniformResourceIdentifier
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus

/**
 * Bridge between kotlin Status and java CredentialStatus.
 */
object MDocStatusBridge {

    @JvmStatic
    fun create(credentialStatus: CredentialStatus): Status =
        Status(
            statusList = Status.StatusListInfo(
                credentialStatus.statusList().index.toULong(),
                UniformResourceIdentifier(credentialStatus.statusList().uri().toString())
            )
        )

    @JvmStatic
    fun extractIndex(status: Status): Int =
        status.statusList?.index?.toInt() ?: -1

    @JvmStatic
    fun extractUri(status: Status): String? =
        status.statusList?.uri?.string

}
