package com.fldsmdfr.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

/** Empty container: the info screen has no slots, but Forge needs a server-side container to open a GUI. */
public class ContainerInfo extends Container {
    @Override
    public boolean canInteractWith(EntityPlayer player) { return true; }
}
