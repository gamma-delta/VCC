package me.gammadelta.common.utils;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.item.ItemDebugoggles;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class Utils {
    /**
     * Generate a random list of numbers from [0, n), each appearing exactly once.
     */
    public static int[] randomIndices(int count, Random rand) {
        int[] out = new int[count];
        // Fill the array with the numbers in sequence
        for (int n = 0; n < count; n++) {
            out[n] = rand.nextInt(count);
        }
        // Shuffle using a "Fisher-Yates" shuffle, whatever that is
        for (int n = count - 1; n > 0; n--) {
            int idx = rand.nextInt(n + 1);
            // Swap them
            int swapper = out[idx];
            out[idx] = out[n];
            out[n] = swapper;
        }

        return out;
    }

    /**
     * Get a random value from a nested array.
     * Sadly this is O(n).
     */
    public static <T> T getRandomNested(ArrayList<ArrayList<T>> list, Random rand) {
        int size = list.stream().mapToInt(ArrayList::size).sum();
        return getRandomNested(list, size, rand);
    }

    /**
     * Get a random value from a nested array if you already know the size.
     * Sadly this is O(n).
     */
    public static <T> T getRandomNested(ArrayList<ArrayList<T>> list, int size, Random rand) {
        int randIdx = rand.nextInt(size);
        for (ArrayList<T> innerList : list) {
            for (T elem : innerList) {
                if (randIdx == 0) {
                    return elem;
                }
                randIdx--;
            }
        }

        // this will never happen
        throw new IllegalStateException();
    }

    /**
     * Uncertainly index into a nested array.
     * It will randomly pick one of the inner ones.
     */
    public static <T> T getUncertainNested(ArrayList<ArrayList<T>> list, int index, Random rand) {
        for (ArrayList<T> innerList : list) {
            index -= innerList.size();
            if (index < 0) {
                // here's our stop, return this element.
                int randIdx = rand.nextInt(innerList.size());
                return innerList.get(randIdx);
            }
        }

        // we went too far!
        throw new IndexOutOfBoundsException();
    }

    /**
     * Uncertainly index an int into a nested array.
     * It will randomly pick one of the inner ones.
     */
    public static int getUncertainNestedInt(ArrayList<IntList> list, int index, Random rand) {
        for (IntList innerList : list) {
            index -= innerList.size();
            if (index < 0) {
                // here's our stop, return this element.
                int randIdx = rand.nextInt(innerList.size());
                return innerList.getInt(randIdx);
            }
        }

        // we went too far!
        throw new IndexOutOfBoundsException();
    }

    /**
     * Intelligently parse a BigInteger, allowing for underscores and leading 0x or 0b.
     */
    public static BigInteger parseBigInt(String input) throws NumberFormatException {
        String deunderscored = input.replace("_", "");
        if (input.startsWith("0x")) {
            return new BigInteger(deunderscored.substring(2), 16);
        } else if (input.startsWith("0b")) {
            return new BigInteger(deunderscored.substring(2), 2);
        } else {
            return new BigInteger(deunderscored);
        }
    }

    /**
     * Intelligently parse an integer, allowing for underscores and leading 0x or 0b.
     */
    public static int parseInt(String input) throws NumberFormatException {
        String deunderscored = input.replace("_", "");
        if (input.startsWith("0x")) {
            return Integer.parseInt(deunderscored.substring(2), 16);
        } else if (input.startsWith("0b")) {
            return Integer.parseInt(deunderscored.substring(2), 2);
        } else {
            return Integer.parseInt(deunderscored);
        }
    }

    private static final boolean DEBUG_MODE = true;

    public static void debugf(String format, Object... params) {
        if (DEBUG_MODE) {
            System.out.printf(format, params);
        }
    }

    /**
     * Given a player and their hand (from an activation), get the debug activation level.
     * This is the level of Bane of Arthropods they have, or 0 if they don't have any.
     */
    public static int funniDebugLevel(PlayerEntity player, Hand hand) {
        // https://minecraft.gamepedia.com/Player.dat_format#Enchantments
        ItemStack heldItem = player.getHeldItem(hand);
        // ok, intellij says this can't be null, but what happens if you aren't holding anything?
        // czech mate, libarls
        if (heldItem == null) {
            return 0;
        }
        ListNBT enchants = heldItem.getEnchantmentTagList();
        for (INBT ienchTag : enchants) {
            CompoundNBT enchTag = (CompoundNBT) ienchTag;
            if (enchTag == null) {
                continue;
            }
            String enchID = enchTag.getString("id");
            if (enchID.equals("minecraft:bane_of_arthropods")) {
                // this returns 0 if it can't find the level.
                // oh what would I do for an actually good Option in Java...
                return enchTag.getInt("lvl");
            }
        }
        // there were enchantments but no BoAs
        return 0;
    }

    /**
     * Given a map of block positions to distances from some center,
     * find the shortest distance of a new block position.
     * <p>
     * This only searches blocks it already knows about.
     * <p>
     * It also inserts the new block position with the new distance as the key.
     * <p>
     * Returns -1 if it can't find the block or anything adjacent to it.
     * <p>
     * It should be O(1).
     */
    public static int findDistance(AbstractObject2IntMap<BlockPos> distances, BlockPos needle) {
        if (distances.containsKey(needle)) {
            return distances.getInt(needle);
        } else {
            int smallestNeighbor = Arrays.stream(Direction.values()).mapToInt(dir -> {
                BlockPos neighbor = needle.offset(dir);
                if (distances.containsKey(neighbor)) {
                    return distances.getInt(neighbor);
                } else {
                    return Integer.MAX_VALUE;
                }
            }).min().getAsInt(); // this will never fire because Direction.values() is not 0 length
            if (smallestNeighbor == Integer.MAX_VALUE) {
                // we didn't find anything ;(
                return -1;
            } else {
                // hey we have a neighbor here
                distances.put(needle, smallestNeighbor + 1);
                return smallestNeighbor + 1;
            }
        }
    }

    /**
     * Sort a list of (T, Distance) pairs into batches.
     */
    public static <T> ArrayList<ArrayList<T>> batchByDistance(List<Pair<T, Integer>> original) {
        ArrayList<ArrayList<T>> out = new ArrayList<>();
        ArrayList<T> currentBatch = new ArrayList<>();
        int currentDistance = -1;
        for (Pair<T, Integer> pair : original) {
            T val = pair.getFirst();
            int distance = pair.getSecond();
            if (currentDistance == -1) {
                // this is the first iteration
                currentDistance = distance;
            } else if (currentDistance != distance) {
                // we need to move onto the next slot
                out.add(currentBatch);
                currentBatch = new ArrayList<>();
            }
            currentBatch.add(val);
        }
        if (!currentBatch.isEmpty()) {
            out.add(currentBatch);
        }
        return out;
    }

    /**
     * Possibly update debugoggles.
     * Call this in an onBlockActivated method.
     */
    public static ActionResultType updateDebugoggles(@Nullable TileMotherboard mother, PlayerEntity player) {
        // (3 is the index for the head)
        ItemStack headStack = player.inventory.armorInventory.get(3);
        if (headStack.getItem() == VCCItems.DEBUGOGGLES.get()) {
            // the player is wearing debugoggles
            // Make the debugoggles select this
            if (mother != null) {
                CompoundNBT tag = headStack.getOrCreateTag();
                boolean changed = false;
                if (!tag.contains(ItemDebugoggles.MOTHERBOARD_POS_KEY)) {
                    changed = true;
                } else {
                    BlockPos oldPos = NBTUtil.readBlockPos(tag.getCompound(ItemDebugoggles.MOTHERBOARD_POS_KEY));
                    changed = !oldPos.equals(mother.getPos());
                }
                tag.put(ItemDebugoggles.MOTHERBOARD_POS_KEY, NBTUtil.writeBlockPos(mother.getPos()));
                if (changed) {
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return ActionResultType.PASS;
    }
}
