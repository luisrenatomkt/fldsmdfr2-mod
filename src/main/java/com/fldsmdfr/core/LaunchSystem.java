package com.fldsmdfr.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.entity.EntityFoodDrop;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/** Fires physical food from the launcher pods that are drawn on the machine body. */
public final class LaunchSystem {
    private static final double GRAVITY = 0.04;

    private LaunchSystem() {}

    /** Counts food entities in the world and removes the oldest above the limit. Returns the remaining count. */
    public static int recount(World w, int max) {
        List<EntityFoodDrop> list = new ArrayList<EntityFoodDrop>();
        for (Entity e : w.loadedEntityList) {
            if (e instanceof EntityFoodDrop && !e.isDead) list.add((EntityFoodDrop) e);
        }
        if (list.size() <= max) return list.size();
        Collections.sort(list, new Comparator<EntityFoodDrop>() {
            @Override
            public int compare(EntityFoodDrop a, EntityFoodDrop b) { return b.ticksExisted - a.ticksExisted; }
        });
        int remove = list.size() - max;
        for (int i = 0; i < remove; i++) list.get(i).setDead();
        return max;
    }

    /** One launch cycle. Returns the number of entities spawned. */
    public static int update(WorldServer w, BlockPos core, int facing, MachineState st, double dt, int activeFood) {
        if (!st.running) return 0;
        st.launchCooldown -= dt;
        if (st.launchCooldown > 0) return 0;
        double sat = st.sat();
        double interval = 40.0 / (1.0 + 3.0 * sat);
        if (st.surgeTicks > 0) interval *= 0.5;
        interval = Math.max(4.0, interval);
        st.launchCooldown = interval;

        st.recompute();
        double tier = st.tier();
        double scale = MachineModel.scale(tier);
        BigNum rate = st.capacityPerSec().min(st.stats.launchPerSec.mul(ModConfig.d("productionIntensity")));
        rate = rate.mul(ModConfig.d("stormIntensity") * (st.surgeTicks > 0 ? 2.0 + 3.0 * sat : 1.0));
        if (rate.isZero()) return 0;
        st.launched = st.launched.add(rate.mul(interval / 20.0));

        double cx = core.getX() + 0.5, cy = core.getY(), cz = core.getZ() + 0.5;
        EntityPlayer near = w.getClosestPlayer(cx, cy, cz, ModConfig.d("simulationDistance"));
        if (near == null) return 0; // logical only while nobody is close

        int room = Math.min(ModConfig.i("maxLaunchesPerUpdate"), ModConfig.i("maxActiveFood") - activeFood);
        if (room <= 0) return 0;
        double want = 1.0 + 1.5 * rate.log10OnePlus() * (1.0 + 2.0 * sat) * (st.surgeTicks > 0 ? 2.0 : 1.0);
        int entities = (int) Math.min((double) room, Math.floor(want));
        if (entities <= 0) return 0;

        int rendered = MachineModel.renderCount(tier, ModConfig.i("visualComponents"), st.stats.components);
        double facingDeg = EnumFacingAngle.of(facing);
        double range = (ModConfig.d("launchDistance") + st.stats.rangeBonus) * (1.0 + 0.3 * Math.log(scale)) * (1.0 + 0.5 * sat);
        range = Math.min(range, 600.0);
        double baseSize = MachineModel.foodSize(tier, ModConfig.d("foodSizeMultiplier"));
        Random r = st.rng;

        for (int i = 0; i < entities; i++) {
            double x, y, z, dirAngle;
            long k = MachineModel.findLauncher(r, st.seed, rendered);
            if (k >= 0) {
                MachineModel.Slot s = MachineModel.slot(k, st.seed);
                double[] off = MachineModel.toWorldOffset(s.nozzleTip(), scale, facingDeg);
                x = cx + off[0]; y = cy + off[1]; z = cz + off[2];
                dirAngle = Math.atan2(off[2], off[0]);
            } else {
                x = cx; y = cy + 2.3 * scale; z = cz;
                dirAngle = r.nextDouble() * Math.PI * 2.0;
            }
            dirAngle += (r.nextDouble() - 0.5) * (0.5 + 1.5 * sat);

            double size = baseSize * (0.75 + 0.5 * r.nextDouble()) * (st.surgeTicks > 0 ? 1.25 : 1.0);
            EntityFoodDrop e = new EntityFoodDrop(w);
            e.setPosition(x, y, z);
            e.setFood(st.pickFood(r), (float) size);
            e.lifespan = ModConfig.i("foodLifespanTicks");

            double dist = range * (0.25 + 0.75 * Math.pow(r.nextDouble(), 0.7));
            double elev = Math.toRadians(48.0 + r.nextDouble() * 10.0);
            double v = Math.min(Math.sqrt(dist * GRAVITY / Math.sin(2.0 * elev)) * 1.12, 3.6);
            e.motionX = Math.cos(dirAngle) * Math.cos(elev) * v;
            e.motionZ = Math.sin(dirAngle) * Math.cos(elev) * v;
            e.motionY = Math.sin(elev) * v;
            w.spawnEntityInWorld(e);
            if (i == 0 && ModConfig.b("visualEffects")) {
                w.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z, 4, 0.2, 0.1, 0.2, 0.01);
            }
        }
        if (ModConfig.b("sounds") && r.nextInt(3) == 0) {
            w.playSoundEffect(cx, cy + 2 * scale, cz, "tile.piston.out", 0.8f, 0.6f + r.nextFloat() * 0.8f);
        }
        return entities;
    }

    /** Horizontal index (0..3) -> GL rotation angle in degrees (SOUTH=0, WEST=90, NORTH=180, EAST=270). */
    private static final class EnumFacingAngle {
        static double of(int horizontalIndex) { return (horizontalIndex & 3) * 90.0; }
    }
}
