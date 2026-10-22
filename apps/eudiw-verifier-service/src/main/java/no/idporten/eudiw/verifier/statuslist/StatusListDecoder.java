package no.idporten.eudiw.verifier.statuslist;

import no.idporten.eudiw.verifier.VerificationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.InflaterInputStream;

final class StatusListDecoder {
    private static final String ERROR_CODE = "invalid_request";
    private static final Set<Integer> ALLOWED_BITS = Set.of(1, 2, 4, 8);
    private static final int MAX_DECOMPRESSED_BYTES = 1_000_000;

    static int resolveStatus(int idx, int bits, byte[] compressed) {
        if (!ALLOWED_BITS.contains(bits)) {
            throw new VerificationException(ERROR_CODE, "status_list.bits must be one of 1, 2, 4, 8");
        }
        if (idx < 0) {
            throw new VerificationException(ERROR_CODE, "status_list.idx must be non-negative");
        }

        long requiredBytesLong = ((long) idx * bits) / 8L + 1L;
        if (requiredBytesLong > MAX_DECOMPRESSED_BYTES) {
            throw new VerificationException(ERROR_CODE, "status_list.idx exceeds maximum supported range");
        }
        int requiredBytes = (int) requiredBytesLong;

        byte[] statuses;
        try (InflaterInputStream input = new InflaterInputStream(new ByteArrayInputStream(compressed));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            while (output.size() < requiredBytes) {
                int maxRead = Math.min(buffer.length, requiredBytes - output.size());
                int read = input.read(buffer, 0, maxRead);
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
            }
            statuses = output.toByteArray();
        } catch (IOException e) {
            throw new VerificationException(ERROR_CODE, "status_list.lst is not a valid zlib stream");
        }

        int maxEntries = statuses.length * 8 / bits;
        if (idx >= maxEntries) {
            throw new VerificationException(ERROR_CODE, "status_list.idx is out of bounds");
        }

        int bitOffset = idx * bits;
        int byteIndex = bitOffset / 8;
        int bitShift = bitOffset % 8;
        int mask = (1 << bits) - 1;
        return (statuses[byteIndex] >> bitShift) & mask;
    }

    private StatusListDecoder() {
    }
}
