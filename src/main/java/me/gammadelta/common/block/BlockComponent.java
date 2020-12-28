package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.utils.FloodUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;

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
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        TileDumbComputerComponent comp = (TileDumbComputerComponent) world.getTileEntity(pos);
        if (comp == null) {
            return;
        }
        TileMotherboard mother = comp.getMotherboard(world);
        if (mother != null) {
            mother.updateConnectedComponents();
        } else {
            // Go and find my mother
            mother = FloodUtils.findMotherboard(comp);
            if (mother != null) {
                // no NPEs, we know it isn't null because we just checked!
                comp.motherboardLocation = mother.getPos();
                comp.motherboardUUID = mother.getUUID();
                mother.updateConnectedComponents();
            }
        }
    }
}
