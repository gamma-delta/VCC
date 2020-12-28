package me.gammadelta.common.utils;

import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class FloodUtils {
    // TODO: make an actual config handler.
    private static final int MAXIMUM_RECUR_COUNT = 1024;

    /**
     * Give this a motherboard, and it will return the
     * positions of all directly connected, unclaimed components.
     *
     * Claimed components act as air or similar; it will not continue searching
     * upon finding one.
     */
    public static Set<TileDumbComputerComponent> findUnclaimedComponents(TileMotherboard mother) {
        BlockPos original = mother.getPos();
        World world = mother.getWorld();

        Set<TileDumbComputerComponent> found = new HashSet<>();
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (placesSearched.size() < MAXIMUM_RECUR_COUNT && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            TileEntity maybeTE = world.getTileEntity(questioning);
            if (maybeTE instanceof TileDumbComputerComponent) {
                TileDumbComputerComponent comp = (TileDumbComputerComponent) maybeTE;
                if (comp.motherboardLocation != null) {
                    // this one has been claimed
                    continue;
                }
                found.add(comp);
                for (Direction d : Direction.values()) {
                    BlockPos nextPos = maybeTE.getPos().offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            }
        }

        return found;
    }

    /**
     * Flood fill from a newly placed component to find the motherboard.
     * Will search through unclaimed components, but not claimed ones.
     */
    @Nullable
    public static TileMotherboard findMotherboard(TileDumbComputerComponent component) {
        BlockPos original = component.getPos();
        World world = component.getWorld();

        Set<TileDumbComputerComponent> found = new HashSet<>();
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (placesSearched.size() < MAXIMUM_RECUR_COUNT && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            TileEntity maybeTE = world.getTileEntity(questioning);
            if (maybeTE instanceof TileDumbComputerComponent) {
                TileDumbComputerComponent comp = (TileDumbComputerComponent) maybeTE;
                if (comp.motherboardLocation != null) {
                    // this one has been claimed
                    continue;
                }
                found.add(comp);
                for (Direction d : Direction.values()) {
                    BlockPos nextPos = maybeTE.getPos().offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            } else if (maybeTE instanceof TileMotherboard) {
                // we found it!
                return (TileMotherboard) maybeTE;
            }
        }

        return null; // :pensive:
    }
}
