package me.gammadelta.vcc.common.block;

import me.gammadelta.vcc.VCCMod;
import me.gammadelta.vcc.common.block.tile.TileMotherboard;
import me.gammadelta.vcc.common.network.MsgHighlightBlocks;
import me.gammadelta.vcc.common.utils.FloodUtils;
import me.gammadelta.vcc.common.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Collections;

public class BlockComponent extends Block {
    public static Properties PROPERTIES = Properties.create(Material.IRON)
            .sound(SoundType.METAL)
            .harvestLevel(1)
            .harvestTool(ToolType.PICKAXE)
            .hardnessAndResistance(2.0f);

    public BlockComponent() {
        super(PROPERTIES);
        this.setDefaultState(getDefaultState().with(BlockStateProperties.LIT, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return getLit(context.getPos(), context.getWorld());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockStateProperties.LIT);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        // Go and find my mother
        TileMotherboard mother = FloodUtils.findMotherboard(pos, world);
        if (mother != null) {
            mother.updateConnectedComponents();
        }
    }

    @Override
    public void onPlayerDestroy(IWorld worldIn, BlockPos pos, BlockState state) {
        TileMotherboard mother = FloodUtils.findMotherboard(pos, worldIn);
        if (mother != null) {
            mother.updateConnectedComponents();
        }
    }

    @Override
    public void onExplosionDestroy(World worldIn, BlockPos pos, Explosion explosionIn) {
        TileMotherboard mother = FloodUtils.findMotherboard(pos, worldIn);
        if (mother != null) {
            mother.updateConnectedComponents();
        }
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        TileMotherboard mother = FloodUtils.findMotherboard(pos, worldIn);
        return onBlockActivated(mother, state, worldIn, pos, player, handIn, hit);
    }

    /**
     * This is a separate method so subclasses can call this supermethod without having each super call
     * have to go find the motherboard.
     */
    public ActionResultType onBlockActivated(@Nullable TileMotherboard mother, BlockState state, World worldIn,
            BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        ActionResultType res = ActionResultType.PASS;

        int debugLevel = Utils.funniDebugLevel(player, handIn);
        if (debugLevel > 0 && mother != null && !worldIn.isRemote) {
            VCCMod.getNetwork()
                    .send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new MsgHighlightBlocks(
                            Collections.singletonList(mother.getPos()), mother.getUUID()
                    ));
            res = ActionResultType.SUCCESS;
        }
        ActionResultType debugogglesRes = Utils.updateDebugoggles(mother, player);
        if (debugogglesRes.isSuccess()) {
            res = debugogglesRes;
        }
        return res;
    }

    // On a block update, check if this is lit.
    @Override
    public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn,
            BlockPos currentPos, BlockPos facingPos) {
        return getLit(stateIn, currentPos, worldIn);
    }

    // don't move on piston push
    @Override
    public PushReaction getPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }

    @Override
    public int getLightValue(BlockState state, IBlockReader world, BlockPos pos) {
        return state.get(BlockStateProperties.LIT) ? 13 : 0;
    }

    /**
     * Check whether this block ought to be lit and return a new updated state.
     * <p>
     * i mean lmao 420 blaze it
     */
    public BlockState getLit(BlockState original, BlockPos pos, IWorld world) {
        TileMotherboard mother = FloodUtils.findMotherboard(pos, world);
        return original.with(BlockStateProperties.LIT, mother != null);
    }

    public BlockState getLit(BlockPos pos, IWorld world) {
        return getLit(getDefaultState(), pos, world);
    }
}
