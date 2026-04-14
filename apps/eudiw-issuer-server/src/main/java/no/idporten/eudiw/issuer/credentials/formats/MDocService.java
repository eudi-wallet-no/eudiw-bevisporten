package no.idporten.eudiw.issuer.credentials.formats;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import id.walt.mdoc.COSECryptoProviderKeyInfo;
import id.walt.mdoc.SimpleCOSECryptoProvider;
import id.walt.mdoc.dataelement.*;
import id.walt.mdoc.doc.MDoc;
import id.walt.mdoc.doc.MDocBuilder;
import id.walt.mdoc.mso.DeviceKeyInfo;
import id.walt.mdoc.mso.Status;
import id.walt.mdoc.mso.ValidityInfo;
import kotlin.time.Instant;
import no.idporten.eudiw.issuer.ErrorCode;
import no.idporten.eudiw.issuer.IssuerServerException;
import no.idporten.eudiw.issuer.config.CredentialIssuerTenant;
import no.idporten.eudiw.issuer.credentials.configurations.ExtendedCredentialConfiguration;
import no.idporten.eudiw.issuer.credentials.status.CredentialStatus;
import no.idporten.eudiw.issuer.credentials.types.*;
import no.idporten.eudiw.issuer.openid4vci.protocol.Credential;
import no.idporten.lib.keystore.KeyProvider;
import no.idporten.lib.keystore.KeystoreManager;
import org.cose.java.CoseException;
import org.cose.java.OneKey;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MDocService {

    private final KeystoreManager keystoreManager;

    public MDocService(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    public Credential issueCredential(CredentialIssuerTenant credentialIssuer, JWK jwk, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims, CredentialStatus credentialStatus) {
        String mDoc = encode(createMDoc(jwk, credentialConfiguration, claims, credentialStatus));
        //System.out.println("Issuing credential " + mDoc);
        return Credential.builder().credential(mDoc).build();
    }

    protected String encode(MDoc mDoc) {
        return Base64.getUrlEncoder().encodeToString(mDoc.getIssuerSigned().toMapElement().toCBOR());
    }

    protected MDoc createMDoc(JWK jwk, ExtendedCredentialConfiguration credentialConfiguration, List<Claim> claims, CredentialStatus credentialStatus) {
        var ISSUER_KEY_ID = "ISSUER_KEY";
        var DEVICE_KEY_ID = "DEVICE_KEY";
        var READER_KEY_ID = "READER_KEY";
        KeyProvider keyProvider = keystoreManager.getKeyProvider(credentialConfiguration.getCredentialIssuerContext().getCredentialSigningKeystore());
        String docType = credentialConfiguration.getCredentialType();

        SimpleCOSECryptoProvider cryptoProvider = new SimpleCOSECryptoProvider(
                List.of(
                        new COSECryptoProviderKeyInfo(
                                ISSUER_KEY_ID,
                                org.cose.java.AlgorithmID.ECDSA_256,
                                keyProvider.publicKey(),
                                keyProvider.privateKey(),
                                List.of((X509Certificate) keyProvider.certificate()),
                                List.of((X509Certificate) keyProvider.certificate())) // TODO root certs
                ));
        DeviceKeyInfo deviceKeyInfo = new DeviceKeyInfo(DataElement.Companion.fromCBOR(convertKey(jwk)),
                null,
                null);

        MDocBuilder mDocBuilder = new MDocBuilder(docType);
        for (Claim entry : claims) {
            DataElement data = getDataElement(entry.getValue());
            mDocBuilder.addItemToSign(entry.getPath().getFirst(), entry.getPath().getLast(), data);
        }
        // TODO hack for iOS wallet mdoc issue timestamp validation failure
        Instant signedAt = Instant.Companion.fromEpochMilliseconds(Clock.systemUTC().instant().minus(1, ChronoUnit.MINUTES).toEpochMilli());
        Instant validFrom = signedAt;
        Instant validTo = Instant.Companion.fromEpochMilliseconds(Clock.systemUTC().instant().plus(credentialConfiguration.getCredentialIssuerContext().getValidityDays(), ChronoUnit.DAYS).toEpochMilli());
        Instant expectedUpdateAt = validTo;
        Status mdocStatus = credentialStatus != null ? MDocStatusBridge.create(credentialStatus) : null;
        return mDocBuilder.sign(
                new ValidityInfo(signedAt, validFrom, validTo, expectedUpdateAt),
                deviceKeyInfo,
                cryptoProvider,
                ISSUER_KEY_ID,
                mdocStatus
        );
    }

    private static byte[] convertKey(JWK jwk) {
        try {
            return new OneKey(jwk.toECKey().toECPublicKey(), null).AsCBOR().EncodeToBytes();
        } catch (CoseException | JOSEException e) {
            throw new IssuerServerException(ErrorCode.SERVER_ERROR, "Failed to convert JWK to COSE key, cannot create mdoc", e);
        }
    }

    private static DataElement getDataElement(ClaimValue claimValue) {
        switch (claimValue) {
            case StringValue(String value) -> {
                return new StringElement(value);
            }
            case BooleanValue(Boolean value) -> {
                return new BooleanElement(value);
            }
            case NumberValue(Long value) -> {
                return new NumberElement(value);
            }
            case BinaryValue(String value, _) -> { // mimeType is only for SD-JWT, ignored for MDoc.
                byte[] imageAsBytes = Base64.getDecoder().decode(value);
                return new ByteStringElement(imageAsBytes);
            }
            case DateTimeValue(ZonedDateTime value) -> {
                Instant datetime = Instant.Companion.fromEpochMilliseconds(value.toEpochSecond() * 1000);
                return new DateTimeElement(datetime, DEDateTimeMode.tdate);
            }
            case FullDateValue(LocalDate value) -> {
                kotlinx.datetime.LocalDate kxDate = new kotlinx.datetime.LocalDate(
                        value.getYear(),
                        value.getMonthValue(),
                        value.getDayOfMonth()
                );
                return new FullDateElement(kxDate, DEFullDateMode.full_date_str);
            }
            case ListValue(List<ClaimValue> listValue) -> {
                List<DataElement> dataElements = new ArrayList<>();
                for (ClaimValue v : listValue) {
                    dataElements.add(getDataElement(v)); // risky?
                }
                return new ListElement(dataElements);
            }
            case MapValue(Map<String, ClaimValue> m) -> {
                Map<MapKey, DataElement> map = new HashMap<>();
                for (Map.Entry<String, ClaimValue> mapEntry : m.entrySet()) {
                    if (mapEntry.getValue() instanceof StringValue(String value)) {
                        map.put(new MapKey(mapEntry.getKey()), new StringElement(value));
                    } else if (mapEntry.getValue() instanceof FullDateValue(LocalDate value)) {
                        kotlinx.datetime.LocalDate kxDate = new kotlinx.datetime.LocalDate(
                                value.getYear(),
                                value.getMonthValue(),
                                value.getDayOfMonth()
                        );
                        map.put(new MapKey(mapEntry.getKey()), new FullDateElement(kxDate, DEFullDateMode.full_date_str));
                    } else {
                        throw new IllegalArgumentException("Unsupported map value type: " + mapEntry.getValue().getClass());
                    }
                }
                return new MapElement(map);
            }
            case null, default -> {
                throw new IllegalArgumentException("Unsupported data element type: " + claimValue);
            }
        }

    }
}
