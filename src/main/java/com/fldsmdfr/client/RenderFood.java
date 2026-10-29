package com.fldsmdfr.client;

import com.fldsmdfr.entity.EntityFoodDrop;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

/** Draws the physical foods as real 3D models; tumbling in the air, resting on the ground. */
public class RenderFood extends Render<EntityFoodDrop> {
    private final Gfx gfx = new Gfx();

    public RenderFood(RenderManager manager) {
        super(manager);
        this.shadowSize = 0.0f;
    }

    @Override
    public void doRender(EntityFoodDrop e, double x, double y, double z, float yaw, float pt) {
        float sc = e.getFoodScale();
        int id = e.getEntityId();
        float t = e.ticksExisted + pt;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);

        float spinY = (id * 47) % 360 + (e.onGround ? 0f : t * 9f);
        GlStateManager.rotate(spinY, 0f, 1f, 0f);
        if (!e.onGround) {
            GlStateManager.translate(0f, 0.3f * sc, 0f);
            GlStateManager.rotate(t * 7f + id * 13, 1f, 0f, 0.4f);
            GlStateManager.translate(0f, -0.3f * sc, 0f);
        }
        GlStateManager.scale(sc, sc, sc);

        gfx.clear();
        FoodModels.draw(gfx, e.getFoodType(), id);
        gfx.flush();

        GlStateManager.enableCull();
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFoodDrop entity) { return null; }
}
