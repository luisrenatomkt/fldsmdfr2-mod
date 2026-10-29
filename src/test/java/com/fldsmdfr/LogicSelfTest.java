package com.fldsmdfr;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.fldsmdfr.core.BigNum;
import com.fldsmdfr.core.MachineModel;
import com.fldsmdfr.core.MachineState;
import com.fldsmdfr.core.MachineStats;
import com.fldsmdfr.core.ModuleType;

/** Plain-Java self test of the Minecraft-independent logic (see docs/TESTING.md). */
public class LogicSelfTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        if (!ok) { fails++; System.out.println("FAIL: " + msg); } else System.out.println("ok:   " + msg);
    }

    public static void main(String[] a) {
        // ---- BigNum
        BigNum x = BigNum.ONE;
        for (int i = 0; i < 1000; i++) x = x.mul(1e10);
        check(x.log10() > 9990 && x.log10() < 10010, "BigNum: 1e10000 without overflow (log10=" + x.log10() + ")");
        check(x.add(BigNum.ONE).compareTo(x) >= 0, "BigNum: adding 1 to 1e10000 stays ordered");
        check(BigNum.of(Double.NaN).isZero() && BigNum.of(Double.POSITIVE_INFINITY).toDouble() > 1e300, "BigNum: NaN/Infinity safe");
        check(BigNum.of(1234567).format().equals("1.23M"), "BigNum: abbreviation " + BigNum.of(1234567).format());
        check(Math.abs(BigNum.fromLog10(400).lnOnePlus() - 400 * Math.log(10)) < 1e-6, "BigNum: lnOnePlus of 1e400");

        // ---- stats: production depends on the number of components; bonus bounded
        MachineStats s = new MachineStats();
        s.recompute(BigNum.of(4), 99L, 0);
        BigNum c4 = s.capacityPerSec;
        s.recompute(BigNum.of(400), 99L, 0);
        BigNum c400 = s.capacityPerSec;
        s.recompute(BigNum.fromLog10(500), 99L, 0);
        check(c400.compareTo(c4) > 0, "stats: more components -> more production (" + c4 + " -> " + c400 + ")");
        check(s.capacityPerSec.log10() > 495 && s.capacityPerSec.log10() < 510, "stats: 1e500 components -> finite capacity " + s.capacityPerSec);
        check(s.integration >= 1.0 && s.integration <= 1.75 + 1e-9, "stats: integration bonus within [1,1.75]: " + s.integration);
        double sum = 0;
        for (double v : s.share) sum += v;
        check(Math.abs(sum - 1.0) < 1e-9, "stats: type shares sum to 1");
        MachineStats s2 = new MachineStats();
        s2.recompute(BigNum.of(400), 99L, 5);
        check(Math.abs(s2.integration - s.integration) > 1e-9 || s2.share[0] != s.share[0], "stats: composition changes with the era");

        // ---- visual evolution has no limit and keeps changing
        check(MachineModel.scale(1000) > MachineModel.scale(100) && MachineModel.scale(100) > MachineModel.scale(10), "model: body scale keeps growing");
        check(!Double.isInfinite(MachineModel.scale(1e9)), "model: scale finite for tier 1e9");
        check(MachineModel.foodSize(1000, 1) > MachineModel.foodSize(100, 1) && MachineModel.foodSize(100, 1) > MachineModel.foodSize(10, 1)
                && MachineModel.foodSize(10, 1) > MachineModel.foodSize(0, 1), "model: food size grows with evolution (0:" + MachineModel.foodSize(0, 1)
                + " 10:" + MachineModel.foodSize(10, 1) + " 100:" + MachineModel.foodSize(100, 1) + " 1000:" + MachineModel.foodSize(1000, 1) + ")");
        Set<Float> hues = new HashSet<Float>();
        for (int e = 0; e < 200; e++) hues.add(MachineModel.eraHue(12345L, e));
        check(hues.size() > 150, "model: every era has its own colour scheme (" + hues.size() + " distinct of 200)");
        check(MachineModel.eraHue(12345L, 7) == MachineModel.eraHue(12345L, 7), "model: era style deterministic");
        check(MachineModel.renderCount(1e6, 220, BigNum.fromLog10(300)) == 220, "model: detail capped by config");
        check(MachineModel.renderCount(0, 220, BigNum.of(4)) == 4, "model: few pods at the start");
        check(MachineModel.era(5.99) == 0 && MachineModel.era(6.0) == 1 && MachineModel.era(61) == 10, "model: era boundaries");

        // ---- pods: stable, distinct, outside the body, launchers exist
        Set<String> pos = new HashSet<String>();
        boolean dup = false, inside = false;
        for (long k = 0; k < 2000; k++) {
            MachineModel.Slot p = MachineModel.slot(k, 777L);
            MachineModel.Slot q = MachineModel.slot(k, 777L);
            if (p.theta != q.theta || p.radius != q.radius || p.y != q.y) dup = true;
            if (p.radius < 1.0) inside = true;
            pos.add(String.format("%.4f,%.4f,%.4f", p.center()[0], p.center()[1], p.center()[2]));
        }
        check(!dup, "pods: slot(k) is deterministic, adding pods never moves old ones");
        check(!inside, "pods: all pods start outside the body (radius >= 1.0)");
        check(pos.size() == 2000, "pods: 2000 distinct positions");
        Random r = new Random(1);
        long found = MachineModel.findLauncher(r, 777L, 200);
        check(found >= 0 && ModuleType.of(found, 777L) == ModuleType.LAUNCHER, "pods: a launcher pod is found (k=" + found + ")");
        MachineModel.Slot ls = MachineModel.slot(found, 777L);
        double[] tip = ls.nozzleTip();
        check(Math.hypot(tip[0], tip[2]) > ls.radius, "pods: cannon tip is outside the pod");
        double[] w = MachineModel.toWorldOffset(new double[]{1, 0, 0}, 2.0, 90);
        check(Math.abs(w[0]) < 1e-9 && Math.abs(w[2] + 2.0) < 1e-9, "pods: facing rotation matches GL rotate about +Y");

        // ---- full simulation: no NaN/Infinity over years, accelerating, persistent growth
        MachineState st = new MachineState();
        st.seed = 4242L;
        double t5 = 0, t60 = 0, tDay = 0;
        boolean finite = true, monotonic = true;
        BigNum prevE = st.evolution;
        long ticks = 0;
        double step = 200;           // 10 seconds
        for (long i = 0; i < 20L * 365 * 24 * 360; i++) {
            if (i > 2_000_000) step = 36000; // switch to 30 min steps after a while
            st.advance(step);
            ticks += (long) step;
            if (st.evolution.compareTo(prevE) < 0) monotonic = false;
            prevE = st.evolution;
            double T = st.tier();
            if (Double.isNaN(T) || Double.isInfinite(T) || Double.isNaN(st.instability)) finite = false;
            if (t5 == 0 && ticks >= 6000) t5 = T;
            if (t60 == 0 && ticks >= 72000) t60 = T;
            if (tDay == 0 && ticks >= 1728000) tDay = T;
            if (ticks > 20L * 365 * 24 * 72000) break;
            if (i > 2_000_000 && ticks > 20L * 365 * 24 * 72000) break;
        }
        System.out.println("      tier after 5 min=" + String.format("%.2f", t5) + ", 1 h=" + String.format("%.2f", t60)
                + ", 1 day=" + String.format("%.1f", tDay) + ", end=" + String.format("%.1f", st.tier())
                + " (era " + st.era() + "), instability=" + String.format("%.3g", st.instability)
                + ", produced=" + st.produced + ", scale=" + String.format("%.2f", MachineModel.scale(st.tier())));
        check(finite, "simulation: no NaN/Infinity over ~20 years of play");
        check(monotonic, "simulation: evolution never decreases");
        check(t60 > t5 && tDay > t60, "simulation: evolution keeps growing (5min " + t5 + " < 1h " + t60 + " < 1day " + tDay + ")");
        check(st.era() > 20, "simulation: keeps producing new eras (era " + st.era() + ")");

        System.out.println(fails == 0 ? "ALL TESTS PASSED" : (fails + " FAILURES"));
        if (fails != 0) System.exit(1);
    }
}
