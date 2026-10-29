package com.fldsmdfr.core;

/** Deterministic 64-bit mixer (SplitMix64) used by the procedural layout. */
public final class Hash {
    private Hash() {}

    public static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform value in [0, bound). */
    public static int bounded(long h, int bound) {
        return (int) Long.remainderUnsigned(h, bound);
    }
}
