package com.fldsmdfr.core;

/** Display helpers. */
public final class Fmt {
    private Fmt() {}

    public static String time(long ticks) {
        long s = ticks / 20;
        long d = s / 86400, h = (s / 3600) % 24, m = (s / 60) % 60, sec = s % 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m " + sec + "s";
        return m + "m " + sec + "s";
    }

    public static String num(double v) { return BigNum.of(v).format(); }

    public static String num(long v) { return BigNum.of((double) v).format(); }
}
