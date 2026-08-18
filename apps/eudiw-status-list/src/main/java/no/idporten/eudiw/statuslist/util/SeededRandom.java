package no.idporten.eudiw.statuslist.util;


public class SeededRandom {
    private long state;

    public SeededRandom(long seed) {
        this.state = seed == 0 ? 0xDEADBEEFCAFEL : seed;
    }

    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException();
        }
        state ^= (state << 13);
        state ^= (state >>> 7);
        state ^= (state << 17);
        return (int) ((state % bound + bound) % bound);
    }
}