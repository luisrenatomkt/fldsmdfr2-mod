package com.fldsmdfr.block;

import net.minecraft.block.Block;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class ModBlocks {
    public static Block core;

    private ModBlocks() {}

    public static void register() {
        core = new BlockCore();
        GameRegistry.registerBlock(core, "fldsmdfr_core");
    }
}
