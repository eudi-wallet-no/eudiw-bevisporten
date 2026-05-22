package no.idporten.eudiw.statuslist.util;

import no.idporten.eudiw.statuslist.exceptions.ErrorCodes;
import no.idporten.eudiw.statuslist.exceptions.StatusListBadRequestException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;

public class StatusListUtil {
    private static final SecureRandom SEED_RNG = new SecureRandom();

    public static int getListId(URI uri) {
        try {
            String[] parts = uri.getPath().split("/");
            return Integer.parseUnsignedInt(parts[parts.length - 1]);
        }
        catch (Exception e) {
            throw new StatusListBadRequestException(
                    ErrorCodes.INVALID_REQUEST,
                    "Unable to parse id from URI: %s".formatted(uri.toString()),
                    e
            );
        }
    }

    public static URI buildUri(String baseUri, int listId) {
        return UriComponentsBuilder.fromUriString(baseUri).buildAndExpand(listId).toUri();
    }

    public static FreeIndexList createFreeIndexList(int length, int seed) {
        return new FreeIndexList(createShuffledArray(length, seed));
    }

    public static int createNewSeed() {
        return SEED_RNG.nextInt(Integer.MAX_VALUE);
    }

    private static int[] createShuffledArray(int length, int seed) {
        int[] numbers = new int[length];
        for (int i = 0; i < length; i++) {
            numbers[i] = i;
        }

        SeededRandom random = new SeededRandom(seed);
        for (int i = length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = numbers[i];
            numbers[i] = numbers[j];
            numbers[j] = temp;
        }

        return numbers;
    }

}
