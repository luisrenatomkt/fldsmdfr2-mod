package com.fldsmdfr.tile;

import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.core.LaunchSystem;
import com.fldsmdfr.core.MachineModel;
import com.fldsmdfr.core.MachineState;
import com.fldsmdfr.core.MachineView;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.world.WorldServer;

/**
 * Server side: runs the logic (evolution, production, launching). Client side: only holds the
 * small {@link MachineView} snapshot used by the renderer and the GUI.
 */
public class TileEntityFldsmdfr extends TileEntity implements ITickable {
    public final MachineState state = new MachineState();
    public final MachineView view = new MachineView();
    public int facing; // horizontal index of the direction the machine front faces

    private int ticks;
    private boolean offlineChecked;
    private int activeFood;

    @Override
    public void update() {
        if (worldObj == null || worldObj.isRemote) return;
        if (state.seed == 0) state.initNew(pos.toLong());
        if (!offlineChecked) {
            offlineChecked = true;
            long elapsed = System.currentTimeMillis() - state.lastActiveMs;
            if (elapsed > 5000) state.applyOffline(elapsed);
            state.lastActiveMs = System.currentTimeMillis();
        }
        ticks++;
        int interval = Math.max(1, ModConfig.i("updateInterval"));
        if (ticks % interval == 0) step(interval);
        if (ticks % Math.max(5, ModConfig.i("syncInterval")) == 0) syncNow();
        if (ticks % 100 == 0) {
            state.lastActiveMs = System.currentTimeMillis();
            markDirty();
        }
    }

    private void step(int interval) {
        state.advance(interval);
        if (state.surgeStarted) {
            state.surgeStarted = false;
            if (ModConfig.b("sounds")) worldObj.playSoundEffect(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, "random.explode", 0.6f, 0.5f);
        }
        if (!state.running) return;
        if (ticks % 40 == 0) activeFood = LaunchSystem.recount(worldObj, ModConfig.i("maxActiveFood"));
        if (worldObj instanceof WorldServer) {
            WorldServer ws = (WorldServer) worldObj;
            activeFood += LaunchSystem.update(ws, pos, facing, state, interval, activeFood);
            effects(ws);
        }
    }

    /** Sparks, smoke and electric arcs between pods; they scale with instability. */
    private void effects(WorldServer w) {
        if (!ModConfig.b("visualEffects")) return;
        double sat = state.sat();
        if (sat < 0.05 || state.rng.nextDouble() > sat * 0.8 + 0.05) return;
        double tier = state.tier();
        double scale = MachineModel.scale(tier);
        int rendered = MachineModel.renderCount(tier, ModConfig.i("visualComponents"), state.stats.components);
        double cx = pos.getX() + 0.5, cy = pos.getY(), cz = pos.getZ() + 0.5;
        double[] p = podWorld(rendered, scale, cx, cy, cz);
        w.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, p[0], p[1] + 0.3, p[2], 2 + (int) (sat * 6), 0.3, 0.2, 0.3, 0.02);
        w.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK, p[0], p[1], p[2], 1 + (int) (sat * 5), 0.4, 0.4, 0.4, 0.05);
        if (state.rng.nextDouble() < sat) {
            double[] q = podWorld(rendered, scale, cx, cy, cz);
            for (int i = 1; i <= 5; i++) {
                double f = i / 6.0;
                w.spawnParticle(EnumParticleTypes.CRIT_MAGIC, p[0] + (q[0] - p[0]) * f + (state.rng.nextDouble() - 0.5) * 0.6,
                        p[1] + (q[1] - p[1]) * f + (state.rng.nextDouble() - 0.5) * 0.6,
                        p[2] + (q[2] - p[2]) * f + (state.rng.nextDouble() - 0.5) * 0.6, 1, 0, 0, 0, 0.0);
            }
        }
        if (ModConfig.b("sounds") && state.rng.nextDouble() < sat * 0.05) {
            w.playSoundEffect(p[0], p[1], p[2], "random.fizz", 0.6f, 0.5f + state.rng.nextFloat());
        }
    }

    private double[] podWorld(int rendered, double scale, double cx, double cy, double cz) {
        if (rendered <= 0) return new double[]{cx, cy + 1.2 * scale, cz};
        MachineModel.Slot s = MachineModel.slot(state.rng.nextInt(rendered), state.seed);
        double[] o = MachineModel.toWorldOffset(s.center(), scale, (facing & 3) * 90.0);
        return new double[]{cx + o[0], cy + o[1], cz + o[2]};
    }

    /** Refreshes the client snapshot and sends it to nearby players. */
    public void syncNow() {
        if (worldObj == null || worldObj.isRemote) return;
        state.fillView(view, activeFood, facing);
        worldObj.markBlockForUpdate(pos);
    }

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound t = new NBTTagCompound();
        state.fillView(view, activeFood, facing);
        view.write(t);
        return new S35PacketUpdateTileEntity(pos, 0, t);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        view.read(pkt.getNbtCompound());
        facing = view.facing;
    }

    @Override
    public void writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        state.lastActiveMs = System.currentTimeMillis();
        state.write(t);
        t.setInteger("facing", facing);
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        state.read(t);
        facing = t.getInteger("facing");
        offlineChecked = false;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        double e = Math.max(4.0, MachineModel.extent(view.rendered, view.era, view.scale));
        return new AxisAlignedBB(pos.getX() + 0.5 - e, pos.getY() - 2, pos.getZ() + 0.5 - e,
                pos.getX() + 0.5 + e, pos.getY() + 3.0 * e / 1.2, pos.getZ() + 0.5 + e);
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        double e = MachineModel.extent(view.rendered, view.era, view.scale);
        double d = Math.max(256.0, e * 6.0);
        return d * d;
    }
}
