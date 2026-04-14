package no.idporten.eudiw.issuer.credentials.formats

import id.walt.mdoc.mso.Status
import id.walt.mdoc.mso.StatusListInfo
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus

/**
 * Bridge between kotlin Status and java CredentialStatus.
 */
object MDocStatusBridge {

    @JvmStatic
    fun create(credentialStatus: CredentialStatus): Status =
        Status(
            null,
            StatusListInfo(
                credentialStatus.statusList().index.toUInt(),
                credentialStatus.statusList().uri().toString(), null
            )
        )

    @JvmStatic
    fun extractIndex(status: Status): Int =
        status.statusList?.index?.toInt() ?: -1;

}