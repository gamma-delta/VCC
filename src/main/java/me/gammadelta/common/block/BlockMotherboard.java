package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

public class BlockMotherboard extends VCCBlock {
    public static final String NAME = "motherboard";

    public BlockMotherboard() {
        super(Properties.create(Material.IRON)
                .sound(SoundType.METAL)
                .harvestLevel(1)
                .harvestTool(ToolType.PICKAXE)
                .hardnessAndResistance(2.0f));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    // i don't have the "proper" nullable installed. oh well.
    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new TileMotherboard();
    }
}
