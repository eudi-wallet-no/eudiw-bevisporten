package no.idporten.eudiw.statuslist.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.SecureRandom;

public class StatusListUtil {
    private static final SecureRandom SEED_RNG = new SecureRandom();

    public static String getListId(URI uri) {
        String[] parts = uri.getPath().split("/");
        return parts[parts.length - 1];
    }

    public static URI buildUri(String baseUri, String listId) {
        return UriComponentsBuilder.fromUriString(baseUri).buildAndExpand(listId).toUri();
    }

    public static URI buildUri(String baseUri, int listId) {
        return buildUri(baseUri, String.valueOf(listId));
    }

    public static FreeIndexList createFreeIndexList(int length, int seed) {
        return new FreeIndexList(createShuffledArray(length, seed));
    }

    public static IntStack createFreeIndexStack(int count, int seed) {
        return createFreeIndexStack(count, seed, count-1);
    }

    public static IntStack createFreeIndexStack(int count, int seed, int next) {
        int[] numbers = createShuffledArray(count, seed);
        return new IntStack(numbers, next);
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
