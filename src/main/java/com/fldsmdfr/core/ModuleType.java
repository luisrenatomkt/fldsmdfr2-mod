package com.fldsmdfr.core;

/**
 * Types of components that grow on the machine body. Only CHAMBER (and the core) produce food,
 * only LAUNCHER (and the core) launch food. The other types matter solely through the
 * explicit integration bonuses computed in {@link MachineStats}.
 */
public enum ModuleType {
    //          id           production launch instability weight
    CHAMBER   ("chamber",    4.0,       0.0,   0.60,       7),
    TANK      ("tank",       0.0,       0.0,   0.20,       5),
    TUBE      ("tube",       0.0,       0.0,   0.10,       6),
    LAUNCHER  ("launcher",   0.0,       6.0,   0.80,       8),
    TURBINE   ("turbine",    0.0,       0.0,   1.00,       4),
    ARM       ("arm",        0.0,       0.0,   0.50,       4),
    TOWER     ("tower",      0.0,       0.0,   0.40,       3),
    CONDUCTOR ("conductor",  0.0,       0.0,   0.30,       4),
    AUX_CORE  ("aux_core",   0.0,       0.0,   1.50,       1),
    PLATFORM  ("platform",   0.0,       0.0,   0.00,       3);

    public final String id;
    public final double production;   // food per second
    public final double launch;       // launched food per second
    public final double instability;  // weight in the instability load
    public final int weight;

    private static final ModuleType[] VALUES = values();
    private static final ModuleType[] BOOT = {CHAMBER, LAUNCHER, TANK, TUBE, CHAMBER, TURBINE, LAUNCHER, ARM};
    private static final int TOTAL_WEIGHT;
    static {
        int t = 0;
        for (ModuleType m : VALUES) t += m.weight;
        TOTAL_WEIGHT = t;
    }

    ModuleType(String id, double production, double launch, double instability, int weight) {
        this.id = id;
        this.production = production;
        this.launch = launch;
        this.instability = instability;
        this.weight = weight;
    }

    public static ModuleType byOrdinal(int i) {
        return (i >= 0 && i < VALUES.length) ? VALUES[i] : CHAMBER;
    }

    /** Deterministic module type for a module index: the first modules follow a fixed bootstrap, then weighted hash. */
    public static ModuleType of(long index, long seed) {
        if (index >= 0 && index < BOOT.length) return BOOT[(int) index];
        int r = Hash.bounded(Hash.mix(index * 0x632BE59BD9B4E019L ^ seed), TOTAL_WEIGHT);
        for (ModuleType m : VALUES) {
            r -= m.weight;
            if (r < 0) return m;
        }
        return CHAMBER;
    }
}
