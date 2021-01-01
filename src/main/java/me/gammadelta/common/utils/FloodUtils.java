package me.gammadelta.common.utils;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.gammadelta.common.block.BlockComponent;
import me.gammadelta.common.block.BlockMotherboard;
import me.gammadelta.common.block.BlockRegister;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public final class FloodUtils {
    // TODO: make an actual config handler.
    private static final int MAXIMUM_RECUR_COUNT = 1024;

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
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (placesSearched.size() < MAXIMUM_RECUR_COUNT && !placesToLook.isEmpty()) {
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
    public static TileMotherboard findMotherboard(BlockPos original, IWorld world) {
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (placesSearched.size() < MAXIMUM_RECUR_COUNT && !placesToLook.isEmpty()) {
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
     */
    public static ArrayList<BlockPos> findRegisters(BlockPos original, BlockState state, IWorld world) {
        ArrayList<BlockPos> found = new ArrayList<>();
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        Direction.Axis axis = state.get(BlockStateProperties.AXIS);
        // We must only fill in the 4 directions that do not point along this axis.
        List<Direction> validDirections = Arrays.stream(Direction.values()).filter(dir -> dir.getAxis() != axis).collect(
                Collectors.toList());

        placesToLook.add(original);

        while (placesSearched.size() < MAXIMUM_RECUR_COUNT && !placesToLook.isEmpty()) {
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
            }
        }

        return found;
    }
}
