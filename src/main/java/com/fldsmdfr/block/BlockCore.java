package com.fldsmdfr.block;

import com.fldsmdfr.FldsmdfrMod;
import com.fldsmdfr.tile.TileEntityFldsmdfr;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

/** The FLDSMDFR machine core. Drawn entirely by the tile entity renderer. */
public class BlockCore extends BlockContainer {
    public BlockCore() {
        super(Material.iron);
        setUnlocalizedName("fldsmdfr.core");
        setHardness(5.0F);
        setResistance(30.0F);
        setStepSound(Block.soundTypeMetal);
        setLightLevel(0.6F);
        setCreativeTab(CreativeTabs.tabRedstone);
    }

    @Override
    public int getRenderType() { return -1; }

    @Override
    public boolean isOpaqueCube() { return false; }

    @Override
    public boolean isFullCube() { return false; }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityFldsmdfr(); }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityFldsmdfr) {
            ((TileEntityFldsmdfr) te).facing = placer.getHorizontalFacing().getOpposite().getHorizontalIndex();
            te.markDirty();
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TileEntityFldsmdfr)) return true;
        TileEntityFldsmdfr m = (TileEntityFldsmdfr) te;
        if (player.isSneaking() && player.getHeldItem() == null) {
            m.state.running = !m.state.running;
            m.markDirty();
            player.addChatMessage(new ChatComponentText("FLDSMDFR: " + (m.state.running ? "ON" : "OFF")));
            m.syncNow();
            return true;
        }
        m.syncNow();
        player.openGui(FldsmdfrMod.instance, 0, world, pos.getX(), pos.getY(), pos.getZ());
        return true;
    }
}
