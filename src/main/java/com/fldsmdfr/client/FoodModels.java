package com.fldsmdfr.client;

/**
 * Procedural 3D models of the foods, drawn with {@link Gfx}. Unit space: base centred on the origin,
 * about 1 unit wide, y up. The entity scale multiplies everything, so a bigger machine tier simply
 * produces bigger food. Indices follow FoodRegistry: burger, pizza, donut, cake, pasta, hotdog,
 * sandwich, fries, icecream. Unknown indices fall back to a coloured ball.
 */
public final class FoodModels {
    private FoodModels() {}

    private static double rnd(long v, int salt) {
        long x = v * 0x9E3779B97F4A7C15L + salt * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        x ^= x >>> 31;
        return (x >>> 11) / (double) (1L << 53);
    }

    public static void draw(Gfx g, int type, long variant) {
        g.glow = false;
        g.alpha = 1f;
        switch (type) {
            case 0: burger(g, variant); break;
            case 1: pizza(g, variant); break;
            case 2: donut(g, variant); break;
            case 3: cake(g, variant); break;
            case 4: pasta(g, variant); break;
            case 5: hotdog(g, variant); break;
            case 6: sandwich(g, variant); break;
            case 7: fries(g, variant); break;
            case 8: iceCream(g, variant); break;
            default:
                g.sphere(0, 0.4, 0, 0.4, 10, (float) rnd(variant, 1), (float) rnd(variant, 2), (float) rnd(variant, 3));
        }
    }

    private static void burger(Gfx g, long v) {
        g.cyl(0, 0.0, 0, 0.48, 0.5, 0.16, 14, 0.86f, 0.58f, 0.27f, true, true);            // bottom bun
        g.cyl(0, 0.16, 0, 0.53, 0.53, 0.14, 14, 0.42f, 0.22f, 0.10f, true, true);          // patty
        g.box(-0.5, 0.30, -0.5, 0.5, 0.33, 0.5, 1.0f, 0.78f, 0.15f);                       // cheese
        g.cyl(0, 0.33, 0, 0.56, 0.56, 0.05, 14, 0.35f, 0.75f, 0.22f, true, true);          // lettuce
        g.cyl(0, 0.38, 0, 0.47, 0.47, 0.05, 14, 0.85f, 0.18f, 0.15f, true, true);          // tomato
        g.ell(0, 0.43, 0, 0.52, 0.42, 0.52, 14, 8, 0.88f, 0.60f, 0.28f, true);             // top bun
        for (int i = 0; i < 9; i++) {
            double a = i * 0.7 + rnd(v, i) , r = 0.12 + 0.28 * rnd(v, 20 + i);
            double x = Math.cos(a) * r, z = Math.sin(a) * r;
            double y = 0.43 + 0.42 * Math.sqrt(Math.max(0, 1 - (x * x + z * z) / (0.52 * 0.52))) - 0.01;
            g.box(x - 0.025, y, z - 0.015, x + 0.025, y + 0.02, z + 0.015, 1f, 0.94f, 0.75f);
        }
    }

    private static void pizza(Gfx g, long v) {
        g.cyl(0, 0, 0, 0.55, 0.55, 0.07, 18, 0.88f, 0.65f, 0.32f, true, true);             // crust base
        g.torus(0, 0.035, 0, 0.5, 0.06, 18, 6, 0.80f, 0.55f, 0.25f, 0, 360);                // rim
        g.cyl(0, 0.07, 0, 0.47, 0.47, 0.02, 18, 0.82f, 0.18f, 0.12f, true, false);          // sauce
        g.cyl(0, 0.09, 0, 0.44, 0.44, 0.03, 18, 1.0f, 0.82f, 0.25f, true, false);           // cheese
        for (int i = 0; i < 9; i++) {
            double a = i * 2.4 + rnd(v, i), r = 0.08 + 0.3 * rnd(v, 30 + i);
            g.cyl(Math.cos(a) * r, 0.12, Math.sin(a) * r, 0.075, 0.075, 0.025, 8, 0.75f, 0.14f, 0.12f, true, false);
        }
        for (int i = 0; i < 6; i++) {
            double a = i * 1.9 + 1.0, r = 0.1 + 0.32 * rnd(v, 50 + i);
            g.box(Math.cos(a) * r - 0.03, 0.12, Math.sin(a) * r - 0.03, Math.cos(a) * r + 0.03, 0.14, Math.sin(a) * r + 0.03, 0.2f, 0.6f, 0.2f);
        }
    }

    private static void donut(Gfx g, long v) {
        g.torus(0, 0.17, 0, 0.30, 0.17, 18, 10, 0.84f, 0.58f, 0.30f, 0, 360);               // dough
        g.torus(0, 0.20, 0, 0.30, 0.175, 18, 10, 0.95f, 0.40f, 0.65f, -10, 190);            // icing
        float[][] cols = {{1f, 1f, 1f}, {0.3f, 0.8f, 1f}, {1f, 0.9f, 0.2f}, {0.4f, 1f, 0.4f}, {0.7f, 0.4f, 1f}};
        for (int i = 0; i < 18; i++) {
            double a = rnd(v, i) * Math.PI * 2, rr = 0.30 + (rnd(v, 40 + i) - 0.5) * 0.22;
            double y = 0.20 + 0.175 * Math.sqrt(Math.max(0, 1 - Math.pow((rr - 0.30) / 0.175, 2)));
            float[] c = cols[i % cols.length];
            g.box(Math.cos(a) * rr - 0.03, y, Math.sin(a) * rr - 0.012, Math.cos(a) * rr + 0.03, y + 0.02, Math.sin(a) * rr + 0.012, c[0], c[1], c[2]);
        }
    }

    private static void cake(Gfx g, long v) {
        g.cyl(0, 0.00, 0, 0.5, 0.5, 0.18, 16, 0.80f, 0.55f, 0.30f, true, true);             // sponge
        g.cyl(0, 0.18, 0, 0.51, 0.51, 0.06, 16, 0.98f, 0.95f, 0.90f, true, true);           // cream
        g.cyl(0, 0.24, 0, 0.5, 0.5, 0.18, 16, 0.80f, 0.55f, 0.30f, true, true);             // sponge
        g.cyl(0, 0.42, 0, 0.52, 0.52, 0.09, 16, 0.98f, 0.45f, 0.65f, true, true);           // pink icing
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            g.sphere(Math.cos(a) * 0.42, 0.54, Math.sin(a) * 0.42, 0.07, 6, 1f, 0.97f, 0.95f);
        }
        g.sphere(0, 0.58, 0, 0.09, 8, 0.85f, 0.08f, 0.12f);                                 // cherry
        g.beam(0, 0.62, 0, 0.05, 0.75, 0.02, 0.015, 0.25f, 0.5f, 0.2f);
    }

    private static void pasta(Gfx g, long v) {
        g.cyl(0, 0, 0, 0.58, 0.45, 0.05, 18, 0.96f, 0.96f, 0.98f, true, true);              // plate
        float[] y = {0.95f, 0.80f, 0.30f};
        for (int i = 0; i < 4; i++) {
            g.torus(0, 0.07 + i * 0.065, 0, 0.30 - i * 0.06, 0.075, 14, 5, y[0], y[1], y[2], 0, 360);
        }
        g.ell(0, 0.30, 0, 0.26, 0.10, 0.26, 12, 6, 0.80f, 0.12f, 0.08f, true);              // sauce
        for (int i = 0; i < 3; i++) {
            double a = i * 2.1 + rnd(v, 1);
            g.sphere(Math.cos(a) * 0.14, 0.38, Math.sin(a) * 0.14, 0.085, 8, 0.45f, 0.22f, 0.12f);
        }
        for (int i = 0; i < 4; i++) {
            double a = i * 1.6;
            g.box(Math.cos(a) * 0.08 - 0.04, 0.44, Math.sin(a) * 0.08 - 0.025, Math.cos(a) * 0.08 + 0.04, 0.455, Math.sin(a) * 0.08 + 0.025, 0.15f, 0.65f, 0.2f);
        }
    }

    private static void hotdog(Gfx g, long v) {
        g.ell(0, 0.15, 0, 0.55, 0.17, 0.22, 14, 8, 0.90f, 0.68f, 0.35f, false);            // bun
        g.push();
        g.translate(0, 0.25, 0);
        g.rotate(90, 0, 0, 1);
        g.cyl(0, -0.62, 0, 0.115, 0.115, 1.24, 10, 0.75f, 0.25f, 0.18f, true, true);       // sausage
        g.pop();
        for (int i = -4; i <= 4; i++) {
            double z = (i % 2 == 0 ? 0.045 : -0.045);
            g.box(i * 0.11 - 0.05, 0.365, z - 0.02, i * 0.11 + 0.05, 0.385, z + 0.02, 1f, 0.85f, 0.1f);
        }
    }

    private static void sandwich(Gfx g, long v) {
        g.box(-0.5, 0.00, -0.4, 0.5, 0.10, 0.4, 0.90f, 0.70f, 0.38f);
        g.box(-0.52, 0.10, -0.42, 0.52, 0.14, 0.42, 0.35f, 0.75f, 0.22f);                   // lettuce
        g.box(-0.48, 0.14, -0.38, 0.48, 0.19, 0.38, 0.90f, 0.22f, 0.18f);                   // tomato
        g.box(-0.5, 0.19, -0.4, 0.5, 0.23, 0.4, 1.0f, 0.80f, 0.20f);                        // cheese
        g.box(-0.49, 0.23, -0.39, 0.49, 0.29, 0.39, 0.95f, 0.55f, 0.58f);                   // ham
        g.box(-0.5, 0.29, -0.4, 0.5, 0.39, 0.4, 0.92f, 0.72f, 0.40f);
        g.beam(0.3, 0.39, 0, 0.3, 0.62, 0, 0.02, 0.8f, 0.7f, 0.4f);                         // toothpick
        g.sphere(0.3, 0.65, 0, 0.05, 6, 0.3f, 0.45f, 0.15f);                                 // olive
    }

    private static void fries(Gfx g, long v) {
        for (int i = 0; i < 18; i++) {
            double a = rnd(v, i) * Math.PI * 2, r = 0.22 * rnd(v, 60 + i);
            double x = Math.cos(a) * r, z = Math.sin(a) * r, h = 0.35 + 0.35 * rnd(v, 90 + i);
            double tx = (rnd(v, 120 + i) - 0.5) * 0.25, tz = (rnd(v, 150 + i) - 0.5) * 0.25;
            g.beam(x, 0.25, z, x + tx, 0.25 + h, z + tz, 0.07, 0.98f, 0.80f, 0.25f);
        }
        g.cyl(0, 0.0, 0, 0.30, 0.42, 0.50, 4, 0.82f, 0.10f, 0.10f, false, true);             // red box
        g.cyl(0, 0.2, 0, 0.36, 0.37, 0.12, 4, 1.0f, 0.85f, 0.15f, false, false);            // yellow stripe
    }

    private static void iceCream(Gfx g, long v) {
        g.cyl(0, 0.0, 0, 0.0, 0.26, 0.55, 10, 0.86f, 0.62f, 0.30f, true, false);            // cone
        g.sphere(0, 0.66, 0, 0.30, 12, 0.98f, 0.60f, 0.75f);                                  // strawberry
        g.sphere(0, 0.98, 0, 0.26, 12, 0.45f, 0.26f, 0.15f);                                  // chocolate
        g.sphere(0.05, 1.22, 0, 0.07, 8, 0.85f, 0.08f, 0.12f);                                // cherry
    }
}
