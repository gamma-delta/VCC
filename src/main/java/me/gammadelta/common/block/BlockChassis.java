package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TileChassis;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public class BlockChassis extends BlockComponent {
    public static final String NAME = "chassis";

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileChassis();
    }
}
