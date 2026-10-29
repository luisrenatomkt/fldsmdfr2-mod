package com.fldsmdfr.core;

import net.minecraft.nbt.NBTTagCompound;

/** Small snapshot sent to clients (a few numbers, once per syncInterval). Drives the renderer and the GUI. */
public final class MachineView {
    private static final String[] ERA_NAMES = {
        "Original", "Aprimoramento", "Conectividade", "Expansao", "Propulsao", "Defesa e poder",
        "Infinito", "Sem limites", "Dominio total", "Onipresenca", "Transcendencia", "Infinito absoluto"};

    public long runtime, seed;
    public BigNum evolution = BigNum.ONE, produced = BigNum.ZERO, components = BigNum.ONE;
    public BigNum launchers = BigNum.ZERO, capacity = BigNum.ZERO;
    public double instability, tier, scale = 1.0, foodSize = 0.7;
    public int surge, activeFood, maxActive, facing, era, rendered;
    public boolean running = true;

    public double sat() { return instability / (1.0 + instability); }

    public String stageName() {
        if (era < ERA_NAMES.length) return "Evolucao " + (era + 1) + " - " + ERA_NAMES[era];
        return "Evolucao " + (era + 1) + " - O Criador";
    }

    public String stateName() {
        if (!running) return "Desligada";
        if (surge > 0) return "SUPERPRODUCAO";
        if (sat() > 0.8) return "Instabilidade critica";
        return "Operacional";
    }

    public void write(NBTTagCompound t) {
        t.setLong("rt", runtime);
        t.setLong("sd", seed);
        evolution.write(t, "ev");
        produced.write(t, "pr");
        components.write(t, "cm");
        launchers.write(t, "ln");
        capacity.write(t, "cp");
        t.setDouble("in", instability);
        t.setDouble("ti", tier);
        t.setDouble("sc", scale);
        t.setDouble("fs", foodSize);
        t.setInteger("su", surge);
        t.setInteger("af", activeFood);
        t.setInteger("ma", maxActive);
        t.setInteger("fc", facing);
        t.setInteger("er", era);
        t.setInteger("rd", rendered);
        t.setBoolean("ru", running);
    }

    public void read(NBTTagCompound t) {
        runtime = t.getLong("rt");
        seed = t.getLong("sd");
        evolution = BigNum.read(t, "ev", BigNum.ONE);
        produced = BigNum.read(t, "pr", BigNum.ZERO);
        components = BigNum.read(t, "cm", BigNum.ONE);
        launchers = BigNum.read(t, "ln", BigNum.ZERO);
        capacity = BigNum.read(t, "cp", BigNum.ZERO);
        instability = t.getDouble("in");
        tier = t.getDouble("ti");
        scale = t.hasKey("sc") ? t.getDouble("sc") : 1.0;
        foodSize = t.getDouble("fs");
        surge = t.getInteger("su");
        activeFood = t.getInteger("af");
        maxActive = t.getInteger("ma");
        facing = t.getInteger("fc");
        era = t.getInteger("er");
        rendered = t.getInteger("rd");
        running = t.getBoolean("ru");
    }
}
