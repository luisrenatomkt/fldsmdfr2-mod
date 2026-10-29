package com.fldsmdfr.entity;

import com.fldsmdfr.core.FoodRegistry;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * A physical, solid-looking piece of food (3D model drawn by client.RenderFood). It cannot be picked
 * up. Size is stored in the data watcher, so the client draws it at the size chosen by the machine.
 * It flies on a ballistic arc, bounces, and rests on the ground until it expires or is culled.
 */
public class EntityFoodDrop extends Entity {
    private static final int DW_TYPE = 12;
    private static final int DW_SCALE = 13;

    public int lifespan = 3600;
    private float appliedScale = -1f;
    private boolean landed;

    public EntityFoodDrop(World world) {
        super(world);
        setSize(0.5f, 0.4f);
        isImmuneToFire = true;
    }

    @Override
    protected void entityInit() {
        dataWatcher.addObject(DW_TYPE, Integer.valueOf(0));
        dataWatcher.addObject(DW_SCALE, Float.valueOf(1.0f));
    }

    public void setFood(int type, float scale) {
        dataWatcher.updateObject(DW_TYPE, Integer.valueOf(type));
        dataWatcher.updateObject(DW_SCALE, Float.valueOf(scale));
        applySize(scale);
    }

    public int getFoodType() { return dataWatcher.getWatchableObjectInt(DW_TYPE); }

    public float getFoodScale() { return dataWatcher.getWatchableObjectFloat(DW_SCALE); }

    /** Collision box is capped (6 blocks) to keep physics cheap; the drawn model keeps growing. */
    private void applySize(float scale) {
        appliedScale = scale;
        FoodRegistry.FoodDef d = FoodRegistry.get(getFoodType());
        float w = (float) Math.max(0.2, Math.min(6.0, scale * d.width));
        float h = (float) Math.max(0.2, Math.min(6.0, scale * d.height));
        double px = posX, py = posY, pz = posZ;
        setSize(w, h);
        setPosition(px, py, pz);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        float sc = getFoodScale();
        if (sc != appliedScale) applySize(sc);
        if (!worldObj.isRemote && ticksExisted > lifespan) {
            setDead();
            return;
        }
        motionY -= 0.04;
        moveEntity(motionX, motionY, motionZ);
        float f = onGround ? 0.6f : 0.99f;
        motionX *= f;
        motionZ *= f;
        motionY *= 0.98;
        if (onGround && motionY < 0) {
            if (!landed && !worldObj.isRemote) {
                landed = true;
                float vol = (float) Math.min(2.5, 0.4 + sc * 0.25);
                worldObj.playSoundAtEntity(this, "mob.slime.big", vol, 0.6f + (float) (Math.random() * 0.4));
            }
            motionY *= -0.3;
            if (Math.abs(motionY) < 0.05) motionY = 0;
        }
    }

    @Override
    public boolean isInRangeToRenderDist(double distance) {
        double r = 96.0 + 24.0 * getFoodScale();
        return distance < r * r;
    }

    /** Foods are transient decoration of the storm: they are not saved with the chunk. */
    @Override
    public boolean writeToNBTOptional(NBTTagCompound tag) { return false; }

    @Override
    protected void readEntityFromNBT(NBTTagCompound t) {
        lifespan = t.getInteger("life");
        dataWatcher.updateObject(DW_TYPE, Integer.valueOf(t.getInteger("type")));
        dataWatcher.updateObject(DW_SCALE, Float.valueOf(t.getFloat("scale")));
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound t) {
        t.setInteger("life", lifespan);
        t.setInteger("type", getFoodType());
        t.setFloat("scale", getFoodScale());
    }
}
