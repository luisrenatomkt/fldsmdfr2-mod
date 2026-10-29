package com.fldsmdfr.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Tiny software geometry recorder: a matrix stack plus box/cylinder/ellipsoid/torus/beam primitives
 * with simple directional shading. Everything is recorded into one array and drawn with a SINGLE
 * Tessellator draw call in {@link #flush()}, so a machine with hundreds of parts costs one draw.
 * Colours are per vertex; there are no textures.
 */
public final class Gfx {
    private float[] d = new float[7 * 8192];
    private int n;
    private double[] m = identity();
    private final double[][] stack = new double[96][];
    private int sp;
    /** Alpha written to every vertex (used by the additive glow batch). */
    public float alpha = 1.0f;
    /** When true, parts are drawn without shading (emissive). */
    public boolean glow;

    private static final double LX = 0.30, LY = 0.82, LZ = 0.48;

    private static double[] identity() {
        return new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};
    }

    public void clear() {
        n = 0;
        sp = 0;
        m = identity();
    }

    public int vertexCount() { return n / 7; }

    // ------------------------------------------------------------------ transforms

    public void push() { stack[sp++] = m.clone(); }

    public void pop() { m = stack[--sp]; }

    public void translate(double x, double y, double z) {
        m[3] += m[0] * x + m[1] * y + m[2] * z;
        m[7] += m[4] * x + m[5] * y + m[6] * z;
        m[11] += m[8] * x + m[9] * y + m[10] * z;
    }

    public void scale(double s) {
        for (int i = 0; i < 12; i++) if (i % 4 != 3) m[i] *= s;
    }

    public void rotate(double deg, double ax, double ay, double az) {
        double l = Math.sqrt(ax * ax + ay * ay + az * az);
        if (l < 1e-9) return;
        ax /= l; ay /= l; az /= l;
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r), t = 1 - c;
        double r00 = t * ax * ax + c, r01 = t * ax * ay - s * az, r02 = t * ax * az + s * ay;
        double r10 = t * ax * ay + s * az, r11 = t * ay * ay + c, r12 = t * ay * az - s * ax;
        double r20 = t * ax * az - s * ay, r21 = t * ay * az + s * ax, r22 = t * az * az + c;
        double[] a = m;
        double n00 = a[0] * r00 + a[1] * r10 + a[2] * r20, n01 = a[0] * r01 + a[1] * r11 + a[2] * r21, n02 = a[0] * r02 + a[1] * r12 + a[2] * r22;
        double n10 = a[4] * r00 + a[5] * r10 + a[6] * r20, n11 = a[4] * r01 + a[5] * r11 + a[6] * r21, n12 = a[4] * r02 + a[5] * r12 + a[6] * r22;
        double n20 = a[8] * r00 + a[9] * r10 + a[10] * r20, n21 = a[8] * r01 + a[9] * r11 + a[10] * r21, n22 = a[8] * r02 + a[9] * r12 + a[10] * r22;
        a[0] = n00; a[1] = n01; a[2] = n02;
        a[4] = n10; a[5] = n11; a[6] = n12;
        a[8] = n20; a[9] = n21; a[10] = n22;
    }

    /** Rotates so that the local +Y axis points along (dx,dy,dz). */
    public void alignY(double dx, double dy, double dz) {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return;
        double cy = dy / len;
        double ax = dz, az = -dx;
        if (ax * ax + az * az < 1e-12) {
            if (cy < 0) rotate(180, 1, 0, 0);
            return;
        }
        rotate(Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, cy)))), ax, 0, az);
    }

    // ------------------------------------------------------------------ low level

    private void vert(double x, double y, double z, float r, float g, float b) {
        if (n + 7 > d.length) d = java.util.Arrays.copyOf(d, d.length * 2);
        d[n++] = (float) (m[0] * x + m[1] * y + m[2] * z + m[3]);
        d[n++] = (float) (m[4] * x + m[5] * y + m[6] * z + m[7]);
        d[n++] = (float) (m[8] * x + m[9] * y + m[10] * z + m[11]);
        d[n++] = r < 0 ? 0 : (r > 1 ? 1 : r);
        d[n++] = g < 0 ? 0 : (g > 1 ? 1 : g);
        d[n++] = b < 0 ? 0 : (b > 1 ? 1 : b);
        d[n++] = alpha;
    }

    private float shade(double nx, double ny, double nz) {
        if (glow) return 1.0f;
        double wx = m[0] * nx + m[1] * ny + m[2] * nz;
        double wy = m[4] * nx + m[5] * ny + m[6] * nz;
        double wz = m[8] * nx + m[9] * ny + m[10] * nz;
        double l = Math.sqrt(wx * wx + wy * wy + wz * wz);
        if (l < 1e-9) return 0.7f;
        double dot = (wx * LX + wy * LY + wz * LZ) / l;
        return (float) (0.48 + 0.52 * Math.max(0.0, dot));
    }

    /** One quad with an explicit outward normal (local space). */
    public void quad(double x0, double y0, double z0, double x1, double y1, double z1,
                     double x2, double y2, double z2, double x3, double y3, double z3,
                     double nx, double ny, double nz, float r, float g, float b) {
        float s = shade(nx, ny, nz);
        r *= s; g *= s; b *= s;
        vert(x0, y0, z0, r, g, b);
        vert(x1, y1, z1, r, g, b);
        vert(x2, y2, z2, r, g, b);
        vert(x3, y3, z3, r, g, b);
    }

    // ------------------------------------------------------------------ primitives

    public void box(double x0, double y0, double z0, double x1, double y1, double z1, float r, float g, float b) {
        quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, r, g, b);
        quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0, r, g, b);
        quad(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, r, g, b);
        quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1, r, g, b);
        quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0, r, g, b);
        quad(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, r, g, b);
    }

    /** Vertical cylinder/cone from y0 to y0+h (r0 at the bottom, r1 at the top). */
    public void cyl(double cx, double y0, double cz, double r0, double r1, double h, int seg,
                    float r, float g, float b, boolean capTop, boolean capBottom) {
        double slope = (r0 - r1) / Math.max(h, 1e-6);
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
            double c0 = Math.cos(a0), s0 = Math.sin(a0), c1 = Math.cos(a1), s1 = Math.sin(a1);
            double am = (a0 + a1) / 2;
            quad(cx + c0 * r0, y0, cz + s0 * r0, cx + c1 * r0, y0, cz + s1 * r0,
                 cx + c1 * r1, y0 + h, cz + s1 * r1, cx + c0 * r1, y0 + h, cz + s0 * r1,
                 Math.cos(am), slope, Math.sin(am), r, g, b);
            if (capTop && r1 > 1e-6) quad(cx, y0 + h, cz, cx + c0 * r1, y0 + h, cz + s0 * r1, cx + c1 * r1, y0 + h, cz + s1 * r1, cx, y0 + h, cz, 0, 1, 0, r, g, b);
            if (capBottom && r0 > 1e-6) quad(cx, y0, cz, cx + c1 * r0, y0, cz + s1 * r0, cx + c0 * r0, y0, cz + s0 * r0, cx, y0, cz, 0, -1, 0, r, g, b);
        }
    }

    /** Ellipsoid; upperOnly draws just the upper half (domes). */
    public void ell(double cx, double cy, double cz, double rx, double ry, double rz, int lon, int lat,
                    float r, float g, float b, boolean upperOnly) {
        int l0 = upperOnly ? lat / 2 : 0;
        for (int j = l0; j < lat; j++) {
            double p0 = Math.PI * j / lat - Math.PI / 2, p1 = Math.PI * (j + 1) / lat - Math.PI / 2;
            for (int i = 0; i < lon; i++) {
                double a0 = 2 * Math.PI * i / lon, a1 = 2 * Math.PI * (i + 1) / lon;
                double am = (a0 + a1) / 2, pm = (p0 + p1) / 2;
                quad(cx + rx * Math.cos(p0) * Math.cos(a0), cy + ry * Math.sin(p0), cz + rz * Math.cos(p0) * Math.sin(a0),
                     cx + rx * Math.cos(p0) * Math.cos(a1), cy + ry * Math.sin(p0), cz + rz * Math.cos(p0) * Math.sin(a1),
                     cx + rx * Math.cos(p1) * Math.cos(a1), cy + ry * Math.sin(p1), cz + rz * Math.cos(p1) * Math.sin(a1),
                     cx + rx * Math.cos(p1) * Math.cos(a0), cy + ry * Math.sin(p1), cz + rz * Math.cos(p1) * Math.sin(a0),
                     Math.cos(pm) * Math.cos(am), Math.sin(pm), Math.cos(pm) * Math.sin(am), r, g, b);
            }
        }
    }

    public void sphere(double cx, double cy, double cz, double rad, int seg, float r, float g, float b) {
        ell(cx, cy, cz, rad, rad, rad, seg, Math.max(4, seg / 2), r, g, b, false);
    }

    /** Torus around the vertical axis. Minor angles in degrees (0..360 = full, 0..180 = upper half). */
    public void torus(double cx, double cy, double cz, double R, double rr, int major, int minor,
                      float r, float g, float b, double minFrom, double minTo) {
        for (int i = 0; i < major; i++) {
            double a0 = 2 * Math.PI * i / major, a1 = 2 * Math.PI * (i + 1) / major, am = (a0 + a1) / 2;
            for (int j = 0; j < minor; j++) {
                double b0 = Math.toRadians(minFrom + (minTo - minFrom) * j / minor);
                double b1 = Math.toRadians(minFrom + (minTo - minFrom) * (j + 1) / minor);
                double bm = (b0 + b1) / 2;
                quad(cx + (R + rr * Math.cos(b0)) * Math.cos(a0), cy + rr * Math.sin(b0), cz + (R + rr * Math.cos(b0)) * Math.sin(a0),
                     cx + (R + rr * Math.cos(b0)) * Math.cos(a1), cy + rr * Math.sin(b0), cz + (R + rr * Math.cos(b0)) * Math.sin(a1),
                     cx + (R + rr * Math.cos(b1)) * Math.cos(a1), cy + rr * Math.sin(b1), cz + (R + rr * Math.cos(b1)) * Math.sin(a1),
                     cx + (R + rr * Math.cos(b1)) * Math.cos(a0), cy + rr * Math.sin(b1), cz + (R + rr * Math.cos(b1)) * Math.sin(a0),
                     Math.cos(bm) * Math.cos(am), Math.sin(bm), Math.cos(bm) * Math.sin(am), r, g, b);
            }
        }
    }

    /** Square-section beam between two points. */
    public void beam(double x0, double y0, double z0, double x1, double y1, double z1, double w, float r, float g, float b) {
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-9) return;
        push();
        translate(x0, y0, z0);
        alignY(dx, dy, dz);
        box(-w / 2, 0, -w / 2, w / 2, len, w / 2, r, g, b);
        pop();
    }

    /** Flat disc in the XY plane at z, facing +Z (used for the screen). */
    public void discZ(double cx, double cy, double z, double rad, int seg, float r, float g, float b, float cr, float cg, float cb) {
        for (int i = 0; i < seg; i++) {
            double a0 = 2 * Math.PI * i / seg, a1 = 2 * Math.PI * (i + 1) / seg;
            float s = shade(0, 0, 1);
            vert(cx, cy, z, Math.min(1f, cr * s), Math.min(1f, cg * s), Math.min(1f, cb * s));
            vert(cx + Math.cos(a0) * rad, cy + Math.sin(a0) * rad, z, r * s, g * s, b * s);
            vert(cx + Math.cos(a1) * rad, cy + Math.sin(a1) * rad, z, r * s, g * s, b * s);
            vert(cx + Math.cos(a1) * rad, cy + Math.sin(a1) * rad, z, r * s, g * s, b * s);
        }
    }

    // ------------------------------------------------------------------ colours / output

    /** HSV -> RGB, all in 0..1. */
    public static float[] hsv(float h, float s, float v) {
        h = (h % 1f + 1f) % 1f;
        float c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), mm = v - c;
        float r, g, b;
        int k = (int) (h * 6);
        switch (k) {
            case 0: r = c; g = x; b = 0; break;
            case 1: r = x; g = c; b = 0; break;
            case 2: r = 0; g = c; b = x; break;
            case 3: r = 0; g = x; b = c; break;
            case 4: r = x; g = 0; b = c; break;
            default: r = c; g = 0; b = x; break;
        }
        return new float[]{r + mm, g + mm, b + mm};
    }

    /** Sends everything recorded so far to the GPU in one draw call and clears the recorder. */
    public void flush() {
        if (n == 0) return;
        Tessellator t = Tessellator.getInstance();
        WorldRenderer wr = t.getWorldRenderer();
        wr.begin(7, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < n; i += 7) {
            wr.pos(d[i], d[i + 1], d[i + 2]).color(d[i + 3], d[i + 4], d[i + 5], d[i + 6]).endVertex();
        }
        t.draw();
        n = 0;
    }
}
