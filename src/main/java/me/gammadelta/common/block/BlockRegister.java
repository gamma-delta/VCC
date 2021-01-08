package me.gammadelta.common.block;

import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.network.MsgHighlightBlocks;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.program.RegisterRepr;
import me.gammadelta.common.utils.Colors;
import me.gammadelta.common.utils.FloodUtils;
import me.gammadelta.common.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;

public class BlockRegister extends BlockComponent {
    public static final String NAME = "register";

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        TileMotherboard tileMotherboard = FloodUtils.findMotherboard(pos, worldIn);
        ActionResultType superAction = super.onBlockActivated(tileMotherboard, state, worldIn, pos, player, handIn,
                hit);

        int debugLevel = Utils.funniDebugLevel(player, handIn);
        if (debugLevel >= 1 && !worldIn.isRemote && tileMotherboard != null) {
            // display this registers friends as the motherboard thinks of them.
            // Note: we could totally just call the flood util,
            // but it's important that we get what the motherboard thinks here
            // for my debugging purposes.
            MotherboardRepr motherboard = tileMotherboard.getMotherboard();
            ArrayList<RegisterRepr> registers = motherboard.registers;
            for (int clusterIdx = 0; clusterIdx < registers.size(); clusterIdx++) {
                RegisterRepr otherRegi = registers.get(clusterIdx);
                for (BlockPos otherPos : otherRegi.manifestations) {
                    if (otherPos.equals(pos)) {
                        // We found our cluster!
                        VCCMod.getNetwork().send(
                                PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                                new MsgHighlightBlocks(
                                        Arrays.asList(otherRegi.manifestations), Colors.REGISTER_RED
                                ));
                        if (debugLevel >= 2) {
                            // Also print out which index this is
                            player.sendMessage(
                                    new TranslationTextComponent("misc.debugNBT.registerIndex", clusterIdx,
                                            registers.size()),
                                    Util.DUMMY_UUID);
                        }
                        // This breaks only the inner loop.
                        // We want to keep going in case two RegisterReprs
                        // somehow own this block.
                        break;
                    }
                }
            }
            return ActionResultType.SUCCESS;
        }

        return superAction;
    }

    // region Blockstate stuff

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction dir = context.getNearestLookingDirection();
        Direction.Axis axis = dir.getAxis();
        return super.getStateForPlacement(context).with(BlockStateProperties.AXIS, axis);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockStateProperties.AXIS);
    }

    // endregion
}
