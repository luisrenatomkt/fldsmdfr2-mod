package com.fldsmdfr.core;

import net.minecraft.nbt.NBTTagCompound;

/**
 * Non-negative number stored as mantissa * 10^exponent (mantissa in [1,10) or zero).
 * The exponent is a long, so values far beyond Double.MAX_VALUE (1e308) never overflow.
 * All operations are NaN/Infinity safe (invalid input collapses to ZERO).
 */
public final class BigNum implements Comparable<BigNum> {
    public static final BigNum ZERO = new BigNum(0.0, 0L);
    public static final BigNum ONE = new BigNum(1.0, 0L);
    private static final String[] SUFFIX = {"", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc"};

    private final double m;
    private final long e;

    private BigNum(double m, long e) {
        this.m = m;
        this.e = e;
    }

    private static BigNum normalize(double m, long e) {
        if (Double.isNaN(m) || Double.isInfinite(m) || m <= 0.0) return ZERO;
        int shift = (int) Math.floor(Math.log10(m));
        double nm = m / Math.pow(10.0, shift);
        long ne = e + shift;
        while (nm >= 10.0) { nm /= 10.0; ne++; }
        while (nm < 1.0) { nm *= 10.0; ne--; }
        return new BigNum(nm, ne);
    }

    public static BigNum of(double v) {
        if (Double.isNaN(v) || v <= 0.0) return ZERO;
        if (Double.isInfinite(v)) v = Double.MAX_VALUE;
        return normalize(v, 0L);
    }

    /** value = 10^l (l may be huge). */
    public static BigNum fromLog10(double l) {
        if (Double.isNaN(l)) return ZERO;
        if (Double.isInfinite(l)) l = l > 0 ? 9.0e18 : -9.0e18;
        double fl = Math.floor(l);
        return normalize(Math.pow(10.0, l - fl), (long) fl);
    }

    public boolean isZero() { return m == 0.0; }

    public double log10() {
        return m == 0.0 ? 0.0 : e + Math.log10(m);
    }

    public double toDouble() {
        if (m == 0.0) return 0.0;
        if (e > 308) return Double.MAX_VALUE;
        if (e < -320) return 0.0;
        double v = m * Math.pow(10.0, e);
        return Double.isInfinite(v) ? Double.MAX_VALUE : v;
    }

    public BigNum add(BigNum o) {
        if (o.isZero()) return this;
        if (isZero()) return o;
        BigNum a = this, b = o;
        if (b.e > a.e || (b.e == a.e && b.m > a.m)) { a = o; b = this; }
        long d = a.e - b.e;
        if (d > 17) return a;
        return normalize(a.m + b.m * Math.pow(10.0, -d), a.e);
    }

    public BigNum add(double v) { return add(of(v)); }

    /** ln(1 + this), safe for any magnitude. */
    public double lnOnePlus() {
        if (isZero()) return 0.0;
        if (e < 15) return Math.log1p(toDouble());
        return Math.log(10.0) * log10();
    }

    /** log10(1 + this). */
    public double log10OnePlus() { return lnOnePlus() / Math.log(10.0); }

    /** min(this, o). */
    public BigNum min(BigNum o) { return compareTo(o) <= 0 ? this : o; }

    public BigNum mul(double k) {
        if (isZero() || Double.isNaN(k) || k <= 0.0) return ZERO;
        if (k > 1e300) k = 1e300;
        return normalize(m * k, e);
    }

    public BigNum mul(BigNum o) {
        if (isZero() || o.isZero()) return ZERO;
        return normalize(m * o.m, e + o.e);
    }

    public BigNum pow(double p) {
        if (isZero()) return ZERO;
        return fromLog10(log10() * p);
    }

    @Override
    public int compareTo(BigNum o) {
        if (isZero() || o.isZero()) return Boolean.compare(!isZero(), !o.isZero());
        if (e != o.e) return e < o.e ? -1 : 1;
        return Double.compare(m, o.m);
    }

    public void write(NBTTagCompound t, String key) {
        t.setDouble(key + "M", m);
        t.setLong(key + "E", e);
    }

    public static BigNum read(NBTTagCompound t, String key, BigNum def) {
        if (!t.hasKey(key + "M")) return def;
        return normalize(t.getDouble(key + "M"), t.getLong(key + "E"));
    }

    /** Human readable: plain (<1e3), K/M/B/T... up to Dc (1e33), then scientific notation. */
    public String format() {
        if (isZero()) return "0";
        if (e < 3) {
            double v = toDouble();
            return v >= 100 ? String.format("%.0f", v) : String.format("%.1f", v);
        }
        if (e < 36) {
            int idx = (int) (e / 3);
            double v = m * Math.pow(10.0, e - idx * 3L);
            return String.format("%.2f%s", v, SUFFIX[idx]);
        }
        return String.format("%.3fe%d", m, e);
    }

    @Override
    public String toString() { return format(); }
}
