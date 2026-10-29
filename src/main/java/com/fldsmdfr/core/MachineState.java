package com.fldsmdfr.core;

import java.util.Random;

import com.fldsmdfr.config.ModConfig;

import net.minecraft.nbt.NBTTagCompound;

/**
 * All persistent state of one machine plus the purely logical simulation (no world access).
 * The machine does NOT build blocks: evolution makes its own body grow (see MachineModel and the
 * renderer). Components, production and launch capacity all derive from the evolution value.
 */
public class MachineState {
    public long seed;
    public boolean running = true;
    public long runtimeTicks;
    public BigNum evolution = BigNum.ONE;
    public BigNum produced = BigNum.ZERO;
    public BigNum launched = BigNum.ZERO;
    public double instability;
    public double launchCooldown;
    public int surgeTicks;
    public long lastActiveMs;

    public final MachineStats stats = new MachineStats();

    // transient
    public boolean surgeStarted;
    public final Random rng = new Random();

    public void initNew(long hint) {
        seed = Hash.mix(hint ^ System.nanoTime());
        lastActiveMs = System.currentTimeMillis();
    }

    public double sat() { return instability / (1.0 + instability); }

    public double tier() { return MachineModel.tier(evolution); }

    public int era() { return MachineModel.era(tier()); }

    /** Number of components of the machine: 4 + 2 * E^0.45 (no upper limit). */
    public BigNum components() { return evolution.pow(0.45).mul(2.0).add(4.0); }

    public void recompute() { stats.recompute(components(), seed, era()); }

    /** Food produced per second, including the evolution bonus and the production setting. */
    public BigNum capacityPerSec() {
        return stats.capacityPerSec.mul((1.0 + 0.1 * tier()) * ModConfig.d("productionIntensity"));
    }

    /** Logical simulation for dt ticks. Safe for very large dt (offline) and very large values. */
    public void advance(double dt) {
        if (dt <= 0 || !running) return;
        runtimeTicks += (long) dt;
        recompute();
        double sat = sat();

        double surgeMul = 1.0;
        if (surgeTicks > 0) {
            surgeTicks -= (int) Math.ceil(dt);
            surgeMul = 1.5 + sat;
            if (surgeTicks <= 0) {
                surgeTicks = 0;
                instability *= 0.9; // pressure relief
            }
        } else {
            double p = sat * sat * 0.0005 * ModConfig.d("stormIntensity") * dt;
            if (rng.nextDouble() < Math.min(0.5, p)) {
                surgeTicks = (int) (100 + rng.nextInt(300) * (1.0 + sat));
                surgeStarted = true;
            }
        }

        BigNum capacity = capacityPerSec();
        produced = produced.add(capacity.mul(surgeMul * dt / 20.0));

        // evolution: exponential in time; the rate itself accelerates as the machine grows
        double tier = tier();
        double r = ModConfig.d("evolutionSpeed") * 3.0e-4
                * (1.0 + 0.25 * Math.log(1.0 + tier) + 0.1 * Math.log(1.0 + capacity.log10OnePlus()) + 0.1 * Math.min(instability, 20.0));
        double x = Math.min(r * dt, 50.0);
        evolution = evolution.mul(Math.exp(x));
        evolution = evolution.add(0.0002 * ModConfig.d("evolutionSpeed") * dt);

        double di = ModConfig.d("instabilityRate") * 1.0e-5
                * (0.3 + 0.25 * stats.instabilityLoad.lnOnePlus() + 0.1 * capacity.log10OnePlus())
                * stats.stabilizer * dt;
        if (!Double.isNaN(di) && !Double.isInfinite(di)) instability += di;
        if (Double.isNaN(instability) || instability < 0) instability = 0;
        if (instability > 1.0e12) instability = 1.0e12;
    }

    /** Logical progression for time the machine was not ticking. */
    public void applyOffline(long elapsedMs) {
        if (!ModConfig.b("offlineProgress") || !running) return;
        double ticks = Math.min(elapsedMs / 50.0, ModConfig.d("maxOfflineHours") * 72000.0);
        if (ticks < 400) return;
        int steps = (int) Math.min(240, Math.max(1, ticks / 200.0));
        double dt = ticks / steps;
        for (int i = 0; i < steps; i++) advance(dt);
        surgeTicks = 0;
    }

    /** Food type for a launch: every type is possible, each era favours different ones. */
    public int pickFood(Random r) {
        int n = FoodRegistry.count();
        int era = era();
        double[] w = new double[n];
        double total = 0;
        for (int i = 0; i < n; i++) {
            w[i] = 0.4 + MachineModel.unit(seed ^ (era * 0x51ED27L), i);
            total += w[i];
        }
        double x = r.nextDouble() * total;
        for (int i = 0; i < n; i++) {
            x -= w[i];
            if (x < 0) return i;
        }
        return 0;
    }

    public void fillView(MachineView v, int activeFood, int facing) {
        recompute();
        double tier = tier();
        v.runtime = runtimeTicks;
        v.seed = seed;
        v.evolution = evolution;
        v.produced = produced;
        v.components = stats.components;
        v.launchers = stats.launchers;
        v.capacity = capacityPerSec();
        v.instability = instability;
        v.tier = tier;
        v.scale = MachineModel.scale(tier);
        v.foodSize = MachineModel.foodSize(tier, ModConfig.d("foodSizeMultiplier"));
        v.surge = surgeTicks;
        v.activeFood = activeFood;
        v.maxActive = ModConfig.i("maxActiveFood");
        v.facing = facing;
        v.era = MachineModel.era(tier);
        v.rendered = MachineModel.renderCount(tier, ModConfig.i("visualComponents"), stats.components);
        v.running = running;
    }

    public void write(NBTTagCompound t) {
        t.setLong("seed", seed);
        t.setBoolean("running", running);
        t.setLong("runtime", runtimeTicks);
        evolution.write(t, "evo");
        produced.write(t, "prod");
        launched.write(t, "laun");
        t.setDouble("instab", instability);
        t.setDouble("cooldown", launchCooldown);
        t.setInteger("surge", surgeTicks);
        t.setLong("lastMs", lastActiveMs);
    }

    public void read(NBTTagCompound t) {
        seed = t.getLong("seed");
        running = !t.hasKey("running") || t.getBoolean("running");
        runtimeTicks = t.getLong("runtime");
        evolution = BigNum.read(t, "evo", BigNum.ONE);
        if (evolution.isZero()) evolution = BigNum.ONE;
        produced = BigNum.read(t, "prod", BigNum.ZERO);
        launched = BigNum.read(t, "laun", BigNum.ZERO);
        instability = t.getDouble("instab");
        if (Double.isNaN(instability) || instability < 0) instability = 0;
        launchCooldown = t.getDouble("cooldown");
        surgeTicks = t.getInteger("surge");
        lastActiveMs = t.getLong("lastMs");
    }
}
