package com.fldsmdfr;

import com.fldsmdfr.block.ModBlocks;
import com.fldsmdfr.command.CommandFldsmdfr;
import com.fldsmdfr.config.ModConfig;
import com.fldsmdfr.entity.EntityFoodDrop;
import com.fldsmdfr.gui.GuiHandler;
import com.fldsmdfr.proxy.CommonProxy;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod(modid = FldsmdfrMod.MODID, name = "FLDSMDFR", version = FldsmdfrMod.VERSION, acceptedMinecraftVersions = "[1.8.9]")
public class FldsmdfrMod {
    public static final String MODID = "fldsmdfr";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static FldsmdfrMod instance;

    @SidedProxy(clientSide = "com.fldsmdfr.proxy.ClientProxy", serverSide = "com.fldsmdfr.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        ModConfig.load(e.getSuggestedConfigurationFile());
        ModBlocks.register();
        proxy.preInit();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        GameRegistry.registerTileEntity(TileEntityFldsmdfr.class, MODID + ":machine");
        EntityRegistry.registerModEntity(EntityFoodDrop.class, "food_ball", 0, this, 128, 3, true);
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        GameRegistry.addRecipe(new ItemStack(ModBlocks.core),
                "IGI", "RCR", "IHI",
                'I', Blocks.iron_block, 'G', Blocks.glass, 'R', Items.redstone, 'C', Items.cake, 'H', Blocks.hopper);
        proxy.init();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandFldsmdfr());
    }
}
