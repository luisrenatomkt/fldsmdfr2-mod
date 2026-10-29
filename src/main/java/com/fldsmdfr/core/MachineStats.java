package com.fldsmdfr.core;

/**
 * Production/launch capacity derived from the number of components grown on the machine.
 * The share of each component type depends on the era (deterministic hash), so complementary
 * ratios (tanks per chamber, turbines per launcher...) change as the machine evolves.
 * All counts are BigNum, so nothing overflows however long the machine evolves.
 */
public final class MachineStats {
    public static final double BASE_CORE_PRODUCTION = 3.0; // food/s per core
    public static final double BASE_CORE_LAUNCH = 2.0;     // launched food/s per core

    public final double[] share = new double[ModuleType.values().length];
    private long shareSeed = Long.MIN_VALUE;
    private int shareEra = Integer.MIN_VALUE;

    public BigNum components = BigNum.ONE;
    public BigNum cores = BigNum.ONE;
    public BigNum launchers = BigNum.ZERO;
    public BigNum capacityPerSec = BigNum.ZERO;
    public BigNum launchPerSec = BigNum.ZERO;
    public BigNum instabilityLoad = BigNum.ZERO;
    public double integration = 1.0, rangeBonus, stabilizer = 1.0;

    private static double ratio(double have, double need) {
        return need <= 0 ? 0 : Math.min(1.0, have / need);
    }

    private void computeShares(long seed, int era) {
        ModuleType[] vs = ModuleType.values();
        double total = 0;
        for (int i = 0; i < vs.length; i++) {
            long h = Hash.mix(seed ^ (era * 0x9E3779B97F4A7C15L) ^ (i * 0x632BE59BD9B4E019L));
            double u = (h >>> 11) / (double) (1L << 53);
            share[i] = vs[i].weight * (0.6 + 0.8 * u);
            total += share[i];
        }
        for (int i = 0; i < vs.length; i++) share[i] /= total;
        shareSeed = seed;
        shareEra = era;
    }

    /** Recomputes everything for a machine with n components. */
    public void recompute(BigNum n, long seed, int era) {
        if (seed != shareSeed || era != shareEra) computeShares(seed, era);
        components = n;
        double sCh = share[ModuleType.CHAMBER.ordinal()], sTk = share[ModuleType.TANK.ordinal()];
        double sTube = share[ModuleType.TUBE.ordinal()], sArm = share[ModuleType.ARM.ordinal()];
        double sL = share[ModuleType.LAUNCHER.ordinal()], sTurb = share[ModuleType.TURBINE.ordinal()];
        double sCond = share[ModuleType.CONDUCTOR.ordinal()], sTower = share[ModuleType.TOWER.ordinal()];
        double sPlat = share[ModuleType.PLATFORM.ordinal()], sAux = share[ModuleType.AUX_CORE.ordinal()];

        cores = n.mul(sAux).add(1.0);
        BigNum chambers = n.mul(sCh);
        BigNum raw = cores.mul(BASE_CORE_PRODUCTION).add(chambers.mul(ModuleType.CHAMBER.production));
        integration = 1.0 + 0.30 * ratio(sTk, 0.5 * sCh) + 0.20 * ratio(sTube, sCh) + 0.25 * ratio(sArm, 0.5 * sCh);
        capacityPerSec = raw.mul(integration);

        launchers = n.mul(sL);
        double launchBonus = 1.0 + 0.35 * ratio(sTurb, sL) + 0.20 * ratio(sCond, sTurb);
        launchPerSec = cores.mul(BASE_CORE_LAUNCH).add(launchers.mul(ModuleType.LAUNCHER.launch * launchBonus));
        rangeBonus = 3.0 * launchers.lnOnePlus() + 5.0 * n.mul(sTower).lnOnePlus();

        ModuleType[] vs = ModuleType.values();
        BigNum load = cores.mul(0.5);
        for (int i = 0; i < vs.length; i++) load = load.add(n.mul(share[i] * vs[i].instability));
        instabilityLoad = load;
        stabilizer = 1.0 / (1.0 + 0.1 * n.mul(sPlat).lnOnePlus());
    }
}
