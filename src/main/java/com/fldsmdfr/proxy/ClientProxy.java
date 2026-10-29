package com.fldsmdfr.proxy;

import com.fldsmdfr.FldsmdfrMod;
import com.fldsmdfr.block.ModBlocks;
import com.fldsmdfr.client.RenderCore;
import com.fldsmdfr.client.RenderFood;
import com.fldsmdfr.entity.EntityFoodDrop;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    @Override
    public void init() {
        Minecraft mc = Minecraft.getMinecraft();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityFldsmdfr.class, new RenderCore());
        RenderingRegistry.registerEntityRenderingHandler(EntityFoodDrop.class, new RenderFood(mc.getRenderManager()));
        mc.getRenderItem().getItemModelMesher().register(Item.getItemFromBlock(ModBlocks.core), 0,
                new ModelResourceLocation(FldsmdfrMod.MODID + ":fldsmdfr_core", "inventory"));
    }
}
