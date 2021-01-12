package me.gammadelta.common.block;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.item.ItemDebugoggles;
import me.gammadelta.common.network.MsgHighlightBlocks;
import me.gammadelta.common.program.CPURepr;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.utils.Colors;
import me.gammadelta.common.utils.FloodUtils;
import me.gammadelta.common.utils.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockCPU extends BlockComponent {
    public static final String NAME = "cpu";

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
            Hand handIn, BlockRayTraceResult hit) {
        TileMotherboard tileMotherboard = FloodUtils.findMotherboard(pos, worldIn);
        ActionResultType superAction = super.onBlockActivated(tileMotherboard, state, worldIn, pos, player, handIn,
                hit);

        ItemStack headStack = player.inventory.armorInventory.get(3);
        CompoundNBT headTag = headStack.getOrCreateTag();
        if (tileMotherboard != null && headStack.getItem() instanceof ItemDebugoggles && headTag.contains(
                ItemDebugoggles.MOTHERBOARD_POS_KEY)) {
            // We just right-clicked on a computer part, so presumably we know about the motherboard
            // so we put us the CPU in
            // TODO: this still plays animation of this is already the wanted CPU.
            headTag.put(ItemDebugoggles.CPU_POS_KEY, NBTUtil.writeBlockPos(pos));
            superAction = ActionResultType.SUCCESS;
        }

        int debugLevel = Utils.funniDebugLevel(player, handIn);
        if (debugLevel >= 1 && !worldIn.isRemote && tileMotherboard != null) {
            // display the CPU's IP and SP extender.
            MotherboardRepr motherboard = tileMotherboard.getMotherboard();
            ArrayList<ArrayList<CPURepr>> cpus = motherboard.cpus;
            for (int groupIdx = 0; groupIdx < cpus.size(); groupIdx++) {
                ArrayList<CPURepr> cpuGroup = cpus.get(groupIdx);
                for (CPURepr cpu : cpuGroup) {
                    if (cpu.manifestation.equals(pos)) {
                        displayDebugInfo(cpu, player, debugLevel, groupIdx, cpus);
                    }
                }
                // Don't break in case this block is somehow owned many times.
            }
            superAction = ActionResultType.SUCCESS;
        }
        return superAction;

    }

    /**
     * Display debug info for this block.
     */
    public static void displayDebugInfo(CPURepr cpu, PlayerEntity player, int debugLevel, int groupIdx,
            ArrayList<ArrayList<CPURepr>> cpus) {
        // Found our CPU!
        if (cpu.ipExtender != null) {
            VCCMod.getNetwork().send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new MsgHighlightBlocks(
                            Arrays.asList(cpu.ipExtender.manifestations), Colors.IP_EXTENDER_BLUE
                    ));
        }
        if (cpu.spExtender != null) {
            VCCMod.getNetwork().send(
                    PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player),
                    new MsgHighlightBlocks(
                            Arrays.asList(cpu.spExtender.manifestations), Colors.SP_EXTENDER_ORANGE
                    ));
        }
        if (debugLevel >= 2) {
            // Also print which CPU group this is in
            player.sendMessage(
                    new TranslationTextComponent("misc.debug.cpuGroup", groupIdx, cpus.size()),
                    Util.DUMMY_UUID);
            if (debugLevel >= 3) {
                // Also also print the register indices
                ArrayList<IntList> registers = cpu.registers;
                if (registers.size() == 0) {
                    player.sendMessage(
                            new TranslationTextComponent("misc.debug.cpuRegisterIndices.none"),
                            Util.DUMMY_UUID);
                } else {
                    player.sendMessage(
                            new TranslationTextComponent("misc.debug.cpuRegisterIndices"),
                            Util.DUMMY_UUID);

                    for (int regiGroupIdx = 0; regiGroupIdx < registers.size(); regiGroupIdx++) {
                        IntList indices = registers.get(regiGroupIdx);
                        StringBuilder bob = new StringBuilder();
                        // this isn't confusing
                        for (int idxidx = 0; idxidx < indices.size(); idxidx++) {
                            int regiIdx = indices.get(idxidx);
                            bob.append('#');
                            bob.append(regiIdx);
                            if (idxidx < indices.size() - 1) {
                                bob.append(", ");
                            }
                        }

                        player.sendMessage(
                                new TranslationTextComponent(
                                        "misc.debug.cpuRegisterIndices.row", regiGroupIdx,
                                        bob.toString()),
                                Util.DUMMY_UUID);
                    }
                }
            }
        }
    }

    /**
     * Return the blocks in the (ip extender, sp extender).
     * The members of the pair will be null if there is no extender there.
     * The entire pair will be null if it found another CPU.
     */
    @Nullable
    public static Pair<List<BlockPos>, List<BlockPos>> findExtenders(BlockPos pos, BlockState state,
            IWorldReader world) {
        // Normally the IP is on the "top" and the SP is on the "bottom"
        // aka, IP is on the same side as the axis, SP is on the opposite side.
        Direction facing = state.get(BlockStateProperties.FACING);

        List<BlockPos> IP = null;
        BlockPos ipPos = pos.offset(facing);
        BlockState maybeIPState = world.getBlockState(ipPos);
        if (maybeIPState.getBlock() instanceof BlockRegister) {
            // nice
            IP = FloodUtils.findRegisters(ipPos, maybeIPState, world, pos);
            if (IP == null) {
                return null;
            }
        }

        List<BlockPos> SP = null;
        BlockPos spPos = pos.offset(facing.getOpposite());
        BlockState maybeSPState = world.getBlockState(spPos);
        if (maybeSPState.getBlock() instanceof BlockRegister) {
            // nice
            SP = FloodUtils.findRegisters(spPos, maybeSPState, world, pos);
            if (SP == null) {
                return null;
            }
        }

        return new Pair<>(IP, SP);
    }

    @Override
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        Pair<List<BlockPos>, List<BlockPos>> exts = findExtenders(pos, state, worldIn);
        return exts != null;
    }

    // region Blockstate stuff

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockStateProperties.FACING).add(VCCBlockStates.TICKING);
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction dir = context.getNearestLookingDirection();
        PlayerEntity p = context.getPlayer();
        if (p == null || !p.isSneaking()) {
            dir = dir.getOpposite();
        }
        return super.getStateForPlacement(context)
                .with(BlockStateProperties.FACING, dir)
                .with(VCCBlockStates.TICKING, false);
    }

    // endregion
}
