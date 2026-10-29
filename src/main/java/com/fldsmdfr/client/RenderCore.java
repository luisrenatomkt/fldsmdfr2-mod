package com.fldsmdfr.client;

import com.fldsmdfr.core.MachineModel;
import com.fldsmdfr.core.MachineView;
import com.fldsmdfr.core.ModuleType;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;

/**
 * Draws the machine. The BODY is the original white/orange capsule (screen, keypad, buttons, legs, light fan).
 * Evolution then grows the machine itself, with no limit: more pods (chambers, tanks, cannons, turbines,
 * arms, antennas, conductors, mini cores, platforms), orbit rings with satellites, thrusters, spires and an
 * aura. Every ERA (6 decades of evolution) brings a new colour scheme and new ring/spire layouts derived
 * from the seed, so there is no fixed set of models. The overall scale also keeps growing.
 * Front of the model is +Z.
 */
public class RenderCore extends TileEntitySpecialRenderer<TileEntityFldsmdfr> {
    private final Gfx solid = new Gfx();
    private final Gfx glow = new Gfx();

    @Override
    public void renderTileEntityAt(TileEntityFldsmdfr te, double x, double y, double z, float pt, int destroyStage) {
        MachineView v = te.view;
        double sat = v.sat();
        int era = v.era;
        float time = (te.getWorld() != null ? te.getWorld().getTotalWorldTime() : 0L) + pt;
        boolean on = v.running;
        float flick = on ? (float) (0.78 + 0.22 * Math.sin(time * (0.6 + 6.0 * sat)) * (0.5 + sat)) : 0.15f;
        if (sat > 0.4 && ((int) (time * 0.7) % 11) == 0) flick *= 0.2f;

        float[] acc = Gfx.hsv(MachineModel.eraHue(v.seed, era), 0.85f, 1.0f);

        solid.clear();
        glow.clear();
        glow.glow = true;
        glow.alpha = 0.85f;
        solid.glow = false;
        solid.alpha = 1f;

        double S = v.scale;
        if (on && sat > 0.05) {
            double amp = 0.012 * sat * S;
            solid.translate(Math.sin(time * 9.0) * amp, Math.abs(Math.sin(time * 13.0)) * amp, Math.cos(time * 11.0) * amp);
            glow.translate(Math.sin(time * 9.0) * amp, Math.abs(Math.sin(time * 13.0)) * amp, Math.cos(time * 11.0) * amp);
        }
        solid.scale(S);
        glow.scale(S);

        body(solid, glow, acc, on, flick, time, sat);
        pods(solid, glow, v, acc, time, on);
        rings(solid, glow, v, acc, time);
        extras(solid, glow, v, acc, time, flick);
        arcs(glow, v, acc, time, sat);

        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y, z + 0.5);
        GlStateManager.rotate(EnumFacing.getHorizontal(te.facing & 3).getHorizontalAngle(), 0, 1, 0);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1, 1, 1, 1);
        solid.flush();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 1);
        GlStateManager.depthMask(false);
        glow.flush();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    // ------------------------------------------------------------------ original body

    private void body(Gfx s, Gfx g, float[] acc, boolean on, float flick, float time, double sat) {
        for (int sx = -1; sx <= 1; sx += 2) for (int sz = -1; sz <= 1; sz += 2) {
            s.box(sx * 0.62 - 0.05, 0.05, sz * 0.45 - 0.05, sx * 0.62 + 0.05, 0.4, sz * 0.45 + 0.05, 0.55f, 0.57f, 0.6f);
            s.box(sx * 0.62 - 0.11, 0.0, sz * 0.45 - 0.11, sx * 0.62 + 0.11, 0.06, sz * 0.45 + 0.11, 0.15f, 0.15f, 0.17f);
        }
        s.box(-0.55, 0.3, -0.4, 0.55, 0.37, 0.4, 0.12f, 0.12f, 0.14f);
        for (int i = -3; i <= 3; i++) {
            float b = on ? Math.min(1f, 0.55f + 0.45f * (float) Math.sin(time * 0.5 + i)) * (0.5f + 0.5f * flick) : 0.1f;
            double hh = 0.12 + 0.03 * (3 - Math.abs(i));
            g.box(i * 0.14 - 0.03, 0.3 - hh, -0.03 + i * 0.01, i * 0.14 + 0.03, 0.3, 0.03 + i * 0.01, 0.5f * b, 0.8f * b, b);
        }
        float ar = acc[0], ag = acc[1], ab = acc[2];
        s.box(-0.9, 0.37, -0.7, 0.9, 0.64, 0.7, ar * 0.95f, ag * 0.95f, ab * 0.95f);        // skirt (era accent)
        s.box(-0.85, 0.64, -0.65, 0.85, 0.92, 0.65, 0.95f, 0.95f, 0.93f);
        s.box(-0.8, 0.92, -0.6, 0.8, 1.9, 0.6, 0.96f, 0.96f, 0.94f);
        s.box(-0.7, 1.9, -0.5, 0.7, 2.05, 0.5, 0.95f, 0.95f, 0.93f);
        s.box(-0.5, 2.05, -0.35, 0.5, 2.15, 0.35, 0.93f, 0.93f, 0.91f);
        s.box(-0.4, 0.64, 0.6, -0.28, 1.9, 0.605, ar, ag, ab);
        s.box(0.55, 0.64, 0.6, 0.67, 1.9, 0.605, ar, ag, ab);
        s.box(-0.805, 1.3, -0.4, -0.8, 1.4, 0.4, ar, ag, ab);
        s.box(0.8, 1.3, -0.4, 0.805, 1.4, 0.4, ar, ag, ab);
        s.box(0.8, 1.2, -0.12, 0.95, 1.28, 0.12, 0.6f, 0.62f, 0.65f);
        s.box(0.9, 1.28, -0.12, 0.95, 1.75, 0.12, 0.6f, 0.62f, 0.65f);
        s.box(0.8, 1.75, -0.12, 0.95, 1.83, 0.12, 0.6f, 0.62f, 0.65f);

        s.box(-0.68, 1.02, 0.6, 0.2, 1.82, 0.64, 0.03f, 0.03f, 0.04f);
        float rr = (float) (0.3 + 0.7 * sat), gg = (float) (0.6 * (1.0 - sat) + 0.1), bb = (float) (1.0 - 0.8 * sat);
        // screen disc: blue at the start, shifts toward the era accent as the machine evolves
        s.discZ(-0.24, 1.42, 0.645, 0.3, 24, rr * flick, gg * flick + 0.1f, bb * flick, 0.8f * flick, 0.9f * flick, 1f * flick);
        g.glow = true;
        g.alpha = 0.35f * flick;
        g.discZ(-0.24, 1.42, 0.66, 0.34, 24, ar, ag, ab, ar, ag, ab);
        g.alpha = 0.85f;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) {
            float gl = on ? (0.45f + 0.55f * (float) Math.abs(Math.sin(time * 0.15 + r * 2 + c))) * flick : 0.1f;
            double kx = 0.3 + c * 0.1, ky = 1.62 + r * 0.08;
            s.box(kx, ky, 0.6, kx + 0.06, ky + 0.045, 0.645, 0.1f * gl, 0.95f * gl, 0.2f * gl);
        }
        float gs = on ? (0.6f + 0.4f * (float) Math.sin(time * 0.3)) * flick : 0.1f;
        s.box(0.3, 1.2, 0.6, 0.55, 1.3, 0.645, 0.1f * gs, 0.95f * gs, 0.2f * gs);
        s.box(0.3, 1.4, 0.6, 0.55, 1.45, 0.645, 0.1f, 0.5f, 0.15f);
        s.box(-0.75, 0.7, 0.6, 0.65, 0.86, 0.63, 0.12f, 0.12f, 0.14f);
        for (int i = 0; i < 10; i++) {
            double bx = -0.7 + i * 0.13;
            float k = (float) i / 9f;
            float lit = on ? (i < 2 ? flick : 1f) : 0.3f;
            s.box(bx, 0.73, 0.63, bx + 0.09, 0.83, 0.655, (0.85f - 0.3f * k) * lit, (0.2f + 0.55f * k) * lit, (0.15f + 0.4f * k) * lit);
        }
    }

    // ------------------------------------------------------------------ pods

    private void pods(Gfx s, Gfx g, MachineView v, float[] acc, float time, boolean on) {
        for (int k = 0; k < v.rendered; k++) {
            MachineModel.Slot p = MachineModel.slot(k, v.seed);
            s.push();
            g.push();
            s.translate(0, p.y, 0);
            g.translate(0, p.y, 0);
            s.rotate(-Math.toDegrees(p.theta), 0, 1, 0);
            g.rotate(-Math.toDegrees(p.theta), 0, 1, 0);
            double R = p.radius, z = p.size;
            // strut from the body to the pod
            s.box(0.8, -0.02, -0.02, R - z, 0.02, 0.02, 0.55f, 0.57f, 0.6f);
            switch (p.type) {
                case CHAMBER:
                    s.box(R - z, -z * 0.8, -z, R + z, z * 0.8, z, 0.95f, 0.95f, 0.93f);
                    s.box(R - z, z * 0.3, -z * 1.02, R + z, z * 0.55, z * 1.02, acc[0], acc[1], acc[2]);
                    g.box(R + z * 0.99, -z * 0.35, -z * 0.5, R + z * 1.03, z * 0.1, z * 0.5, acc[0], acc[1], acc[2]);
                    break;
                case TANK:
                    s.cyl(R, -z, 0, z * 0.7, z * 0.7, z * 2.0, 8, 0.55f, 0.75f, 0.9f, true, true);
                    g.cyl(R, -z * 0.3, 0, z * 0.74, z * 0.74, z * 0.5, 8, acc[0], acc[1], acc[2], false, false);
                    break;
                case TUBE:
                    s.beam(R - z, 0, -z * 0.5, R + z * 1.6, 0, -z * 0.5, 0.06, 0.8f, 0.8f, 0.82f);
                    s.beam(R - z, 0, z * 0.5, R + z * 1.6, 0, z * 0.5, 0.06, 0.8f, 0.8f, 0.82f);
                    s.sphere(R + z * 1.6, 0, 0, z * 0.6, 6, 0.6f, 0.62f, 0.65f);
                    break;
                case LAUNCHER: {
                    double a = Math.toRadians(MachineModel.NOZZLE_ANGLE_DEG);
                    double ex = R + MachineModel.NOZZLE_LEN * Math.cos(a), ey = MachineModel.NOZZLE_LEN * Math.sin(a);
                    s.sphere(R, 0, 0, z * 0.9, 8, 0.35f, 0.36f, 0.4f);
                    s.beam(R, 0, 0, ex, ey, 0, z * 1.1, 0.25f, 0.26f, 0.3f);
                    s.box(ex - z * 0.7, ey - 0.02, -z * 0.7, ex + z * 0.7, ey + 0.05, z * 0.7, 0.15f, 0.15f, 0.17f);
                    float f = on ? (0.5f + 0.5f * (float) Math.abs(Math.sin(time * 0.3 + p.k))) : 0.1f;
                    g.box(ex - z * 0.5, ey + 0.05, -z * 0.5, ex + z * 0.5, ey + 0.07, z * 0.5, acc[0] * f, acc[1] * f, acc[2] * f);
                    break;
                }
                case TURBINE:
                    s.sphere(R, 0, 0, z * 0.5, 6, 0.6f, 0.62f, 0.65f);
                    s.push();
                    s.translate(R, 0, 0);
                    s.rotate(time * (8 + (p.k % 5) * 3), 1, 0, 0);
                    s.box(-0.03, -z * 1.5, -0.05, 0.03, z * 1.5, 0.05, 0.7f, 0.72f, 0.75f);
                    s.box(-0.03, -0.05, -z * 1.5, 0.03, 0.05, z * 1.5, 0.7f, 0.72f, 0.75f);
                    s.pop();
                    g.sphere(R, 0, 0, z * 0.3, 6, acc[0], acc[1], acc[2]);
                    break;
                case ARM: {
                    double sw = Math.sin(time * 0.08 + p.k) * z * 1.2;
                    s.sphere(R, 0, 0, z * 0.5, 6, 0.55f, 0.57f, 0.6f);
                    s.beam(R, 0, 0, R + z * 1.4, sw, 0, 0.06, 0.9f, 0.9f, 0.92f);
                    s.beam(R + z * 1.4, sw, 0, R + z * 2.4, sw * 0.2 - z * 0.6, 0, 0.05, 0.9f, 0.9f, 0.92f);
                    s.box(R + z * 2.3, sw * 0.2 - z * 0.9, -z * 0.3, R + z * 2.5, sw * 0.2 - z * 0.5, z * 0.3, acc[0], acc[1], acc[2]);
                    break;
                }
                case TOWER: {
                    double hgt = z * (4.0 + (p.h & 3));
                    s.beam(R, -z, 0, R, hgt, 0, 0.05, 0.75f, 0.77f, 0.8f);
                    s.beam(R, hgt * 0.4, -z * 0.8, R, hgt * 0.4, z * 0.8, 0.03, 0.75f, 0.77f, 0.8f);
                    float bl = ((int) (time / (6 + p.k % 5)) % 2 == 0) ? 1f : 0.25f;
                    g.sphere(R, hgt + 0.03, 0, 0.045, 5, acc[0] * bl, acc[1] * bl, acc[2] * bl);
                    break;
                }
                case CONDUCTOR:
                    s.beam(R, -z * 2, -z * 2, R, z * 2, z * 2, 0.05, 0.5f, 0.52f, 0.55f);
                    g.beam(R, -z * 2, -z * 2, R, z * 2, z * 2, 0.025, acc[0], acc[1], acc[2]);
                    break;
                case AUX_CORE:
                    s.box(R - z * 1.2, -z, -z, R + z * 1.2, z, z, 0.95f, 0.95f, 0.93f);
                    s.box(R - z * 1.2, -z, -z, R + z * 1.2, -z * 0.6, z, acc[0], acc[1], acc[2]);
                    s.box(R - z * 0.7, -z * 0.2, z * 0.98, R + z * 0.4, z * 0.7, z * 1.02, 0.03f, 0.03f, 0.04f);
                    g.sphere(R - z * 0.15, z * 0.25, z * 1.04, z * 0.28, 6, 0.4f, 0.7f, 1f);
                    break;
                case PLATFORM:
                    s.cyl(R, -z * 0.4, 0, z * 2.0, z * 2.0, z * 0.25, 10, 0.5f, 0.52f, 0.56f, true, true);
                    g.torus(R, -z * 0.3, 0, z * 2.0, 0.012, 10, 3, acc[0], acc[1], acc[2], 0, 360);
                    break;
                default:
                    break;
            }
            s.pop();
            g.pop();
        }
    }

    // ------------------------------------------------------------------ rings, extras

    private void rings(Gfx s, Gfx g, MachineView v, float[] acc, float time) {
        int era = v.era;
        int count = Math.min(era, 8);
        double reach = 1.6 + 0.30 * Math.sqrt(v.rendered + 1.0);
        for (int i = 0; i < count; i++) {
            long h = com.fldsmdfr.core.Hash.mix(v.seed + 7919L * (i + 1));
            double R = reach + 0.6 * i + 0.15 * Math.sqrt(era);
            double tiltX = (MachineModel.unit(h, 1) - 0.5) * 50.0, tiltZ = (MachineModel.unit(h, 2) - 0.5) * 50.0;
            double yy = 0.9 + 0.25 * i;
            float[] c2 = Gfx.hsv(MachineModel.eraHue(v.seed, era) + 0.08f * i, 0.8f, 1f);
            s.push();
            g.push();
            s.translate(0, yy, 0);
            g.translate(0, yy, 0);
            s.rotate(tiltX, 1, 0, 0); g.rotate(tiltX, 1, 0, 0);
            s.rotate(tiltZ, 0, 0, 1); g.rotate(tiltZ, 0, 0, 1);
            s.rotate(time * (0.4 + 0.15 * i) * (i % 2 == 0 ? 1 : -1), 0, 1, 0);
            g.rotate(time * (0.4 + 0.15 * i) * (i % 2 == 0 ? 1 : -1), 0, 1, 0);
            s.torus(0, 0, 0, R, 0.035, 32, 4, 0.6f, 0.62f, 0.68f, 0, 360);
            g.torus(0, 0, 0, R, 0.02, 32, 4, c2[0], c2[1], c2[2], 0, 360);
            for (int j = 0; j < 3; j++) {
                double a = Math.PI * 2 * j / 3 + i;
                double sx = Math.cos(a) * R, sz = Math.sin(a) * R;
                s.box(sx - 0.12, -0.08, sz - 0.12, sx + 0.12, 0.08, sz + 0.12, 0.92f, 0.92f, 0.94f);
                s.box(sx - 0.13, 0.0, sz - 0.13, sx + 0.13, 0.04, sz + 0.13, c2[0], c2[1], c2[2]);
                g.sphere(sx, 0.14, sz, 0.05, 5, c2[0], c2[1], c2[2]);
            }
            s.pop();
            g.pop();
        }
    }

    private void extras(Gfx s, Gfx g, MachineView v, float[] acc, float time, float flick) {
        int era = v.era;
        // thrusters under the body (from era 4: "Propulsao")
        int thr = era >= 4 ? Math.min(4, era - 3) : 0;
        for (int i = 0; i < thr; i++) {
            double a = Math.PI * 2 * i / thr + Math.PI / 4;
            double tx = Math.cos(a) * 0.62, tz = Math.sin(a) * 0.45;
            double len = 0.7 + 0.25 * Math.sin(time * 0.8 + i * 2) * flick + 0.15 * Math.min(era, 10);
            s.cyl(tx, 0.28, tz, 0.07, 0.1, 0.1, 8, 0.2f, 0.2f, 0.24f, false, false);
            g.cyl(tx, 0.28 - len, tz, 0.0, 0.09, len, 8, 0.4f, 0.7f, 1.0f, false, false);
        }
        // spires
        int spires = Math.min(era, 8);
        for (int i = 0; i < spires; i++) {
            long h = com.fldsmdfr.core.Hash.mix(v.seed + 104729L * (i + 3));
            double a = MachineModel.unit(h, 1) * Math.PI * 2, r = 0.45 * (0.3 + MachineModel.unit(h, 2));
            double hgt = 0.9 + 0.35 * i + MachineModel.unit(h, 3) * 0.8;
            double bx = Math.cos(a) * r, bz = Math.sin(a) * r;
            s.beam(bx, 2.1, bz, bx, 2.1 + hgt, bz, 0.04, 0.8f, 0.82f, 0.86f);
            float bl = ((int) (time / (5 + i)) % 2 == 0) ? 1f : 0.3f;
            g.sphere(bx, 2.12 + hgt, bz, 0.05, 5, acc[0] * bl, acc[1] * bl, acc[2] * bl);
        }
        // spinning crown of turbines on top
        int turb = Math.min(era + 1, 6);
        for (int i = 0; i < turb; i++) {
            s.push();
            s.translate(0, 2.15 + 0.09 * i, 0);
            s.rotate(time * (6 + 4 * i) * (i % 2 == 0 ? 1 : -1), 0, 1, 0);
            s.box(-0.42, 0, -0.03, 0.42, 0.04, 0.03, 0.55f, 0.57f, 0.6f);
            s.box(-0.03, 0, -0.42, 0.03, 0.04, 0.42, 0.55f, 0.57f, 0.6f);
            s.pop();
        }
        // aura (additive, very faint) and orbiting sparks
        if (era >= 2) {
            double rad = 1.6 + 0.12 * Math.min(era, 20) + 0.15 * Math.sqrt(v.rendered + 1.0);
            g.alpha = 0.05f;
            g.sphere(0, 1.1, 0, rad, 14, acc[0], acc[1], acc[2]);
            g.alpha = 0.85f;
            int stars = Math.min(48, 4 * era);
            for (int i = 0; i < stars; i++) {
                long h = com.fldsmdfr.core.Hash.mix(v.seed + 31337L * (i + 1));
                double a = MachineModel.unit(h, 1) * Math.PI * 2 + time * 0.01 * (1 + MachineModel.unit(h, 2));
                double rr = rad * (0.8 + 0.5 * MachineModel.unit(h, 3));
                double yy = 1.1 + (MachineModel.unit(h, 4) - 0.5) * rad;
                g.box(Math.cos(a) * rr - 0.03, yy, Math.sin(a) * rr - 0.03, Math.cos(a) * rr + 0.03, yy + 0.06, Math.sin(a) * rr + 0.03, acc[0], acc[1], acc[2]);
            }
        }
    }

    /** Electric arcs between pods; more and brighter with instability. */
    private void arcs(Gfx g, MachineView v, float[] acc, float time, double sat) {
        if (sat < 0.15 || v.rendered < 2 || !v.running) return;
        int count = 1 + (int) (sat * 3);
        long q = (long) (time / 3.0);
        for (int i = 0; i < count; i++) {
            long h = com.fldsmdfr.core.Hash.mix(q * 977L + i * 131L + v.seed);
            long k0 = (long) (MachineModel.unit(h, 1) * v.rendered), k1 = (long) (MachineModel.unit(h, 2) * v.rendered);
            double[] a = MachineModel.slot(k0, v.seed).center(), b = MachineModel.slot(k1, v.seed).center();
            double px = a[0], py = a[1], pz = a[2];
            for (int j = 1; j <= 4; j++) {
                double f = j / 4.0;
                double nx = a[0] + (b[0] - a[0]) * f + (MachineModel.unit(h, 10 + j) - 0.5) * 0.5 * (j < 4 ? 1 : 0);
                double ny = a[1] + (b[1] - a[1]) * f + (MachineModel.unit(h, 20 + j) - 0.5) * 0.5 * (j < 4 ? 1 : 0);
                double nz = a[2] + (b[2] - a[2]) * f + (MachineModel.unit(h, 30 + j) - 0.5) * 0.5 * (j < 4 ? 1 : 0);
                g.beam(px, py, pz, nx, ny, nz, 0.03, 0.7f, 0.85f, 1f);
                px = nx; py = ny; pz = nz;
            }
        }
    }
}
