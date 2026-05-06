package no.idporten.eudiw.statuslist.service;

import jakarta.validation.constraints.NotNull;

public class Status {
    public static final int VALID = 0x00;
    public static final int INVALID = 0x01;

    public static @NotNull String getStatus(int status) {
        return switch (status) {
            case VALID -> "VALID";
            case INVALID -> "INVALID";
            default -> "INVALID STATUS CODE";
        };
    }

    public static boolean isSupported(int status) {
        return switch (status) {
            case VALID, INVALID -> true;
            default -> false;
        };
    }
}
