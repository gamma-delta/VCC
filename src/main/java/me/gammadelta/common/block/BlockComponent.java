package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

public abstract class BlockComponent extends VCCBlock {
    public static Properties PROPERTIES = Properties.create(Material.IRON)
            .sound(SoundType.METAL)
            .harvestLevel(1)
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2.0f);

    public BlockComponent() {
        super(PROPERTIES);
    }

    @Override
    public void onPlayerDestroy(IWorld worldIn, BlockPos pos, BlockState state) {
        super.onPlayerDestroy(worldIn, pos, state);
        this.tellMotherboardToRemoveThis(worldIn, pos);
    }

    @Override
    public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
        super.onExplosionDestroy(worldIn, pos, explosionIn);
        this.tellMotherboardToRemoveThis(worldIn, pos);
    }

    private void tellMotherboardToRemoveThis(IWorld world, BlockPos pos) {
        TileDumbComputerComponent me = (TileDumbComputerComponent) world.getTileEntity(pos);
        if (me != null) {
            TileMotherboard motherboard = me.getMotherboard(world);
            if (motherboard != null && motherboard.getUUID() == me.motherboardUUID) {
                motherboard.updateConnectedComponents();
            }
        }
    }
}
