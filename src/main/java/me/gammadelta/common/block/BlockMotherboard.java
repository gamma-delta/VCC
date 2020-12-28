package me.gammadelta.common.block;

import me.gammadelta.common.utils.Utils;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.network.MsgHighlightBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;

public class BlockMotherboard extends VCCBlock {
    public static final String NAME = "motherboard";

    public BlockMotherboard() {
        super(BlockComponent.PROPERTIES);
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

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        ((TileMotherboard) world.getTileEntity(pos)).updateConnectedComponents();
    }

    @SuppressWarnings("deprecation") // minecraft uses "deprecated" for "override but don't call"
    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        if (worldIn.isRemote) {
            return ActionResultType.PASS;
        }

        TileEntity te = worldIn.getTileEntity(pos);
        if (!(te instanceof TileMotherboard)) {
            // not sure how this happened...
            return ActionResultType.PASS;
        }
        TileMotherboard mother = (TileMotherboard) te;

        int debugLevel = Utils.funniDebugLevel(player, handIn);
        if (debugLevel > 0) {
            VCCMod.getNetwork()
                    .send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new MsgHighlightBlocks(
                            mother.getControlledBlocks(), mother.getUUID()
                    ));
            return ActionResultType.SUCCESS;
        }

        return super.onBlockActivated(state, worldIn, pos, player, handIn, hit);
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return state.get(BlockStateProperties.LIT) ? 13 : 0;
    }

    // region Blockstate stuff

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction dir = context.getNearestLookingDirection();
        PlayerEntity p = context.getPlayer();
        if (p == null || !p.isSneaking()) {
            dir = dir.getOpposite();
        }
        return getDefaultState().with(BlockStateProperties.FACING, dir);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.LIT);
    }

    // endregion
}
