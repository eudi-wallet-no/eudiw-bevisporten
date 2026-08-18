package no.idporten.eudiw.login;

import no.idporten.sdk.oidcserver.OAuth2Exception;
import org.springframework.http.HttpStatus;

import java.util.Arrays;

public enum AcrValue {


    IDPORTEN_LOA_SUBSTANTIAL("idporten-loa-substantial"),
    IDPORTEN_LOA_HIGH("idporten-loa-high"),
    EIDAS_LOA_LOW("eidas-loa-low"),
    EIDAS_LOA_SUBSTANTIAL("eidas-loa-substantial"),
    EIDAS_LOA_HIGH("eidas-loa-high");

    private final String value;

    AcrValue(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static AcrValue fromValue(String value) {
        return Arrays.stream(AcrValue.values())
                .filter(acr -> acr.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new OAuth2Exception(OAuth2Exception.INVALID_REQUEST, "Unknown acr value", HttpStatus.BAD_REQUEST.value()));
    }

    public boolean isEuLogin() {
        return this == EIDAS_LOA_LOW || this == EIDAS_LOA_SUBSTANTIAL || this == EIDAS_LOA_HIGH;
    }

    public boolean isNoLogin() {
        return this == IDPORTEN_LOA_SUBSTANTIAL || this == IDPORTEN_LOA_HIGH;
    }

}
