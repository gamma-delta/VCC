package me.gammadelta.common.block;

import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.item.IMemoryStorageItem;
import me.gammadelta.common.program.Emergency;
import me.gammadelta.common.program.MemoryType;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.utils.FloodUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import java.util.ArrayList;

public class BlockMemory extends BlockComponent {
    public static final String XRAM_NAME = "xram";
    public static final String EXRAM_NAME = "exram";
    public static final String RAM_NAME = "ram";
    public static final String ROM_NAME = "rom";

    public final MemoryType memType;

    public BlockMemory(MemoryType type) {
        super();
        this.memType = type;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        TileMotherboard tileMotherboard = FloodUtils.findMotherboard(pos, worldIn);
        ActionResultType superAction = super.onBlockActivated(tileMotherboard, state, worldIn, pos, player, handIn,
                hit);

        ItemStack handStack = player.getHeldItem(handIn);
        if (handStack.getItem() instanceof IMemoryStorageItem && tileMotherboard != null) {
            // Nice! Find where I am in the motherboard's memory
            MotherboardRepr mother = tileMotherboard.getMotherboard();
            ArrayList<BlockPos> motherMemBlocks = mother.memoryLocations.get(this.memType);
            for (int memBlockIdx = 0; memBlockIdx < motherMemBlocks.size(); memBlockIdx++) {
                BlockPos thatPos = motherMemBlocks.get(memBlockIdx);
                if (thatPos.equals(pos)) {
                    // here's my stop!
                    try {
                        int motherMemIdx = mother.getRegionIndex(this.memType, memBlockIdx);
                        IMemoryStorageItem memItem = (IMemoryStorageItem) handStack.getItem();
                        byte[] itemMemory = memItem.getMemory(handStack);
                        System.arraycopy(itemMemory, 0, mother.memory, motherMemIdx, memItem.getMemorySize());
                        superAction = ActionResultType.SUCCESS;
                    } catch (Emergency ignored) { }
                    break;
                }
            }
        }

        return superAction;
    }
}
