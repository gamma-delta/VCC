package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TilePuncher;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

public class BlockPuncher extends Block {
    public static final String NAME = "puncher";

    public BlockPuncher() {
        super(Properties.create(Material.WOOD).hardnessAndResistance(1.5f).harvestTool(ToolType.AXE));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TilePuncher();
    }
}
