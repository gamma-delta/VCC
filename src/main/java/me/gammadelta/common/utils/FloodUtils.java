package me.gammadelta.common.utils;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.gammadelta.common.VCCConfig;
import me.gammadelta.common.block.BlockCPU;
import me.gammadelta.common.block.BlockComponent;
import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.BlockRegister;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public final class FloodUtils {
    /**
     * Give this a motherboard, and it will return the
     * positions of all directly connected components, along with their taxicab distance from the
     * motherboard.
     * <p>
     * If it finds a connected motherboard, it will return null.
     * Note this is different than returning an empty set!
     */
    @Nullable
    public static Object2IntOpenHashMap<BlockPos> findUnclaimedComponents(TileMotherboard mother) {
        BlockPos original = mother.getPos();
        World world = mother.getWorld();

        Object2IntOpenHashMap<BlockPos> found = new Object2IntOpenHashMap<>();
        found.put(mother.getPos(), 0); // the motherboard is 0 from itself
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        placesSearched.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
            placesSearched.add(original.offset(d));
        }

        while (found.size() <= VCCConfig.FLOODFILL_MAX_FOUND.get()
                && placesSearched.size() <= VCCConfig.FLOODFILL_MAX_SEARCH.get()
                && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            Block maybeComponent = world.getBlockState(questioning).getBlock();
            if (maybeComponent instanceof BlockComponent) {
                Utils.findDistance(found, questioning);
                for (Direction d : Direction.values()) {
                    BlockPos nextPos = questioning.offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            } else if (maybeComponent instanceof BlockMotherboard) {
                // uh-oh, is this not me?
                TileMotherboard otherMother = (TileMotherboard) world.getTileEntity(questioning);
                if (otherMother != null && otherMother.getUUID() != mother.getUUID()) {
                    // too bad
                    return null;
                }
            }
        }

        return found;
    }

    /**
     * Flood fill from a newly placed component to find the motherboard.
     * Will search through unclaimed and claimed components.
     */
    @Nullable
    public static TileMotherboard findMotherboard(BlockPos original, IWorldReader world) {
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        placesSearched.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
            placesSearched.add(original.offset(d));
        }

        while (placesSearched.size() <= VCCConfig.FLOODFILL_MAX_SEARCH.get()
                && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            Block maybeComp = world.getBlockState(questioning).getBlock();
            if (maybeComp instanceof BlockComponent) {
                for (Direction d : Direction.values()) {
                    BlockPos nextPos = questioning.offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            } else if (maybeComp instanceof BlockMotherboard) {
                // we found it!
                return (TileMotherboard) world.getTileEntity(questioning);
            }
        }

        return null; // :pensive:
    }

    /**
     * Flood fill out from a register block to find all the register blocks
     * part of the logical register.
     * <p>
     * (Don't fail upon finding a CPU).
     */
    @Nonnull
    public static ArrayList<BlockPos> findRegisters(BlockPos original, BlockState state, IWorldReader world) {
        return Objects.requireNonNull(findRegisters(original, state, world, null));
    }

    /**
     * Flood fill out from a register block to find all the register blocks
     * part of the logical register.
     * <p>
     * For this overload, failOnFindingCPU will make this return null
     * if it finds a CPU not at the parentCPUPos.
     * This is to find IP and SP extenders.
     */
    @Nullable
    public static ArrayList<BlockPos> findRegisters(BlockPos original, BlockState state, IWorldReader world,
            @Nullable BlockPos parentCPUPos) {
        ArrayList<BlockPos> found = new ArrayList<>();
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        Direction.Axis axis = state.get(BlockStateProperties.AXIS);
        // We must only fill in the 4 directions that do not point along this axis.
        List<Direction> validDirections = Arrays.stream(Direction.values())
                .filter(dir -> dir.getAxis() != axis)
                .collect(
                        Collectors.toList());

        placesToLook.add(original);
        placesSearched.add(original);

        while (placesSearched.size() <= VCCConfig.FLOODFILL_MAX_SEARCH.get()
                && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            BlockState qState = world.getBlockState(questioning);
            Block maybeComp = qState.getBlock();
            // add it if it's a register and it has the same axial direction as me
            if (maybeComp instanceof BlockRegister && qState.get(BlockStateProperties.AXIS) == axis) {
                found.add(questioning);
                for (Direction d : validDirections) {
                    BlockPos nextPos = questioning.offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            } else if (maybeComp instanceof BlockCPU && questioning == parentCPUPos) {
                // uh oh we're colliding with another CPU
                return null;
            }
        }

        return found;
    }

    /**
     * Flood fill out from a CPU's location to find the block positions of the closest block in all the register
     * clusters it can see, along with their distances.
     * It will include extenders, so don't include them in the canidates.
     * <p>
     * The algorithm will naturally find the closest register block in a cluster first.
     * It will search through the rest of the blocks in the cluster but not include them in the output.
     */
    public static ArrayList<Pair<BlockPos, Integer>> findCPURegistersAndDistances(BlockPos original,
            Set<BlockPos> canidates, IWorldReader world) {
        ArrayList<Pair<BlockPos, Integer>> found = new ArrayList<>();
        Queue<Pair<BlockPos, Integer>> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();
        Set<BlockPos> ignoreTheseRegisters = new HashSet<>();

        // We are 0 blocks away from ourselves.
        placesToLook.add(new Pair<>(original, 0));
        placesSearched.add(original);

        while (placesSearched.size() <= VCCConfig.FLOODFILL_MAX_SEARCH.get()
                && !placesToLook.isEmpty()) {
            Pair<BlockPos, Integer> pair = placesToLook.remove();
            BlockPos questioning = pair.getFirst();
            int distance = pair.getSecond();
            BlockState state = world.getBlockState(questioning);
            Block maybeRegi = state.getBlock();
            if (maybeRegi instanceof BlockRegister && !ignoreTheseRegisters.contains(questioning)) {
                // noice
                found.add(new Pair<>(questioning, distance));
                // Ignore the rest of the registers in this cluster
                ignoreTheseRegisters.addAll(FloodUtils.findRegisters(questioning, state, world));
            }
            // In any case, add the next things to find to the list
            for (Direction d : Direction.values()) {
                BlockPos nextPos = questioning.offset(d);
                if (!placesSearched.contains(nextPos) && canidates.contains(nextPos)) {
                    placesToLook.add(new Pair<>(nextPos, distance + 1));
                    placesSearched.add(nextPos);
                }
            }
        }

        return found;
    }
}
