package com.fldsmdfr.core;

/**
 * Pure geometry/appearance rules of the machine, shared by the client renderer and the server
 * (launch points come from the SAME launcher pods that are drawn). Nothing here stores state:
 * everything is a deterministic function of (seed, component index, evolution tier).
 *
 * Units: "model units" = blocks at scale 1. The whole model is multiplied by {@link #scale(double)}.
 */
public final class MachineModel {
    public static final double NOZZLE_LEN = 0.45;
    public static final double NOZZLE_ANGLE_DEG = 40.0;
    public static final double ERA_SPAN = 6.0; // decades of evolution per era

    private MachineModel() {}

    /** Evolution tier T = log10(1 + E): unbounded, grows with every power of ten of evolution. */
    public static double tier(BigNum evolution) { return evolution.log10OnePlus(); }

    public static int era(double tier) { return (int) Math.floor(Math.max(0.0, tier) / ERA_SPAN); }

    /** Body scale: grows without limit but slowly (logarithmically). */
    public static double scale(double tier) { return 1.0 + 0.9 * Math.log(1.0 + Math.max(0.0, tier)); }

    /** Number of component pods that are drawn (level of detail); the logical count can be far larger. */
    public static int renderCount(double tier, int cfgMax, BigNum components) {
        int byTier = (int) Math.min(1_000_000.0, 6.0 + 10.0 * Math.max(0.0, tier));
        int c = Math.min(cfgMax, byTier);
        double n = components.toDouble();
        if (n < c) c = (int) Math.max(0, Math.floor(n));
        return Math.max(0, c);
    }

    /** Physical size (in blocks) of the food launched at this tier. Unbounded, grows with evolution. */
    public static double foodSize(double tier, double multiplier) {
        return 0.7 * Math.pow(1.0 + Math.max(0.0, tier), 0.35) * multiplier;
    }

    /** Hue (0..1) of the accent colour of an era; every era gets a new colour scheme. */
    public static float eraHue(long seed, int era) {
        if (era <= 0) return 0.065f; // original orange
        long h = Hash.mix(seed ^ (era * 0xD1B54A32D192ED03L));
        double jitter = ((h >>> 11) / (double) (1L << 53)) * 0.12;
        return (float) (((0.065 + 0.137 * era + jitter) % 1.0 + 1.0) % 1.0);
    }

    /** Approximate half-extent (blocks) of everything drawn, for the render bounding box. */
    public static double extent(int rendered, int era, double scale) {
        double ring = 1.6 + 0.30 * Math.sqrt(rendered + 1.0) + 0.45 * Math.min(era, 8);
        return (ring + 2.5) * scale;
    }

    /** Unit random in [0,1) from a hash and a salt. */
    public static double unit(long h, int salt) {
        long x = Hash.mix(h + salt * 0x9E3779B97F4A7C15L);
        return (x >>> 11) / (double) (1L << 53);
    }

    public static final class Slot {
        public final long k;
        public final ModuleType type;
        public final double theta;   // radians, direction from the body axis
        public final double radius;  // distance of the pod centre from the axis (model units)
        public final double y;       // height of the pod centre
        public final double size;    // pod half-size
        public final long h;         // per-pod hash for variations

        Slot(long k, ModuleType type, double theta, double radius, double y, double size, long h) {
            this.k = k;
            this.type = type;
            this.theta = theta;
            this.radius = radius;
            this.y = y;
            this.size = size;
            this.h = h;
        }

        /** Tip of the cannon of a launcher pod, in model units: {x, y, z} relative to the body axis (before facing). */
        public double[] nozzleTip() {
            double a = Math.toRadians(NOZZLE_ANGLE_DEG);
            double r = radius + NOZZLE_LEN * Math.cos(a);
            return new double[]{Math.cos(theta) * r, y + NOZZLE_LEN * Math.sin(a), Math.sin(theta) * r};
        }

        public double[] center() {
            return new double[]{Math.cos(theta) * radius, y, Math.sin(theta) * radius};
        }
    }

    /** Pod number k. Stable: adding more components never moves the existing ones. */
    public static Slot slot(long k, long seed) {
        long h = Hash.mix(k * 0x9E3779B97F4A7C15L ^ seed);
        double shell = Math.sqrt(k + 1.0);
        double theta = (k * 2.399963229728653 + (unit(h, 1) - 0.5) * 0.35) % (Math.PI * 2.0);
        double radius = 1.0 + 0.30 * shell + unit(h, 2) * 0.18;
        double y = 0.45 + unit(h, 3) * (1.7 + 0.22 * shell);
        double size = 0.16 + 0.14 * unit(h, 4);
        return new Slot(k, ModuleType.of(k, seed), theta, radius, y, size, h);
    }

    /** Rotates a model-space point by the machine facing (same convention as GL rotate about +Y) and scales it. */
    public static double[] toWorldOffset(double[] local, double scale, double facingDeg) {
        double a = Math.toRadians(facingDeg);
        double c = Math.cos(a), s = Math.sin(a);
        double x = local[0] * scale, y = local[1] * scale, z = local[2] * scale;
        return new double[]{x * c + z * s, y, -x * s + z * c};
    }

    /** Index of a random LAUNCHER pod among the first `rendered` pods, or -1 if none was found. */
    public static long findLauncher(java.util.Random r, long seed, int rendered) {
        if (rendered <= 0) return -1;
        for (int i = 0; i < 24; i++) {
            long k = r.nextInt(rendered);
            if (ModuleType.of(k, seed) == ModuleType.LAUNCHER) return k;
        }
        return -1;
    }
}
