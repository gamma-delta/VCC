package me.gammadelta;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Predicate;

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

    public static String hexdump(Iterable<Byte> bytes) {
        StringBuilder bob = new StringBuilder();
        StringBuilder thisLine = new StringBuilder();

        int lineIdx = 0;
        int idx = 0;
        for (Byte theByte : bytes) {
            lineIdx = idx % 16;
            if (lineIdx == 0) {
                // start of line
                String region = String.format("%08x  ", idx);
                bob.append(region);
                thisLine.append('|');
            }
            bob.append(String.format("%02x ", theByte));
            if ((theByte & 0xff) >= 0x20 && (theByte & 0xff) < 0x7f) {
                thisLine.append((char) theByte.byteValue());
            } else {
                thisLine.append('.');
            }

            if (lineIdx == 7) {
                // put spacer between 8 bytes
                bob.append(' ');
            } else if (lineIdx == 15) {
                // end of line!
                thisLine.append('|');
                bob.append(String.format("  %s\n", thisLine.toString()));
                thisLine = new StringBuilder();
            }

            idx++;
        }
        // Append the total length
        if (lineIdx != 15) {
            // Figure out how many characters we lack
            int missingChars = 3 * (15 - lineIdx);
            if (lineIdx < 7) {
                // extra space for the ' ' expected between bytes 7 and 8
                missingChars++;
            }
            for (int i = 0; i < missingChars; i++) {
                bob.append(' ');
            }
            thisLine.append('|');
            bob.append(String.format("  %s\n", thisLine.toString()));
        }
        bob.append(String.format("%1$08x  (%1$d bytes)", idx));

        return bob.toString();
    }

    public static String smolHexdump(Iterable<Byte> bytes) {
        StringBuilder bob = new StringBuilder();
        int count = 0;
        for (byte b : bytes) {
            bob.append(String.format("%02x ", b));
            count++;
        }
        // i think 8 bytes is too many to count at a glance
        if (count > 8) {
            bob.append(String.format("(%d bytes)", count));
        }


        return bob.toString();
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

    /**
     * Turn a byte array into a long
     */
    public static long toLong(ByteList bytes) {
        long out = 0;
        for (byte b : bytes) {
            out <<= 8;
            out |= 0xff & b;
        }
        return out;
    }

    /**
     * Turn a byte array into a long
     */
    public static long toLong(byte[] bytes) {
        long out = 0;
        for (byte b : bytes) {
            out <<= 8;
            out |= 0xff & b;
        }
        return out;
    }

    /**
     * Write the long into the byte array, truncating the MSBs and filling with zeros.
     * Return 0 if there was no stretch/squish, 1 if there was stretch, and -1 if there was squish
     */
    public static int writeLong(long val, byte[] destination) {
        // gotta love java
        BigInteger imLazy = new BigInteger(Long.toString(val));
        byte[] bigBytes = imLazy.toByteArray();
        for (int i = 0; i < destination.length; i++) {
            int destIdx = destination.length - i - 1;
            int bbIdx = bigBytes.length - i - 1;
            if (bbIdx >= 0) {
                destination[destIdx] = bigBytes[bbIdx];
            } else {
                destination[destIdx] = 0;
            }
        }

        return Integer.compare(destination.length, bigBytes.length);
    }

    /**
     * Add two byte lists together, mutating `lhs`.
     * `rhs` is considered to be extended with zeroes.
     * If `rhs` is longer than `lhs`, it will only add the LSBs.
     * Returns any overflow.
     */
    public static ByteList addMut(ByteList lhs, ByteList rhs) {
        // We always want to pad with one zero to the front to avoid
        // things being treated as negative and not overflowing properly.
        BigInteger bigLhs;
        if (lhs.size() > 0) {
            byte[] bytes = new byte[lhs.size() + 1];
            lhs.getElements(0, bytes, 1, lhs.size());
            bigLhs = new BigInteger(bytes);
        } else {
            bigLhs = BigInteger.ZERO;
        }
        BigInteger bigRhs;
        if (rhs.size() > 0) {
            byte[] bytes = new byte[rhs.size() + 1];
            rhs.getElements(0, bytes, 1, rhs.size());
            bigRhs = new BigInteger(bytes);
        } else {
            bigRhs = BigInteger.ZERO;
        }
        BigInteger bigSum = bigLhs.add(bigRhs);
        byte[] sum = bigSum.toByteArray();
        for (int i = 0; i < lhs.size(); i++) {
            int lhsIdx = lhs.size() - i - 1;
            int sumIdx = sum.length - i - 1;
            if (sumIdx >= 0) {
                lhs.set(lhsIdx, sum[sum.length - i - 1]);
            } else {
                // lhs is longer than the sum.
                // check if it's negative?
                if (bigSum.compareTo(BigInteger.ZERO) >= 0) {
                    // pad with 0s
                    lhs.set(lhsIdx, (byte) 0);
                } else {
                    // pad with FFs
                    lhs.set(lhsIdx, (byte) 0xff);
                }
            }

        }
        // we will have remainder if rhs >= lhs
        if (sum.length >= lhs.size()) {
            int remainderSize = sum.length - lhs.size();
            if (remainderSize > 0) {
                return new ByteArrayList(sum, 0, remainderSize);
            } else {
                return ByteLists.EMPTY_LIST;
            }
        } else {
            // no remainder
            return ByteLists.EMPTY_LIST;
        }
    }

    /**
     * Add two byte arrays together. Returns the (overflow, sum).
     */
    public static Pair<ByteList, ByteList> add(byte[] lhs, byte[] rhs) {
        ByteList lhsList = new ByteArrayList(lhs);
        ByteList rem = Utils.addMut(lhsList, new ByteArrayList(rhs));
        return new Pair<>(rem, lhsList);
    }

    /**
     * Perform lhs -= rhs. Return any underflow (aka, how much rhs was left over after subtracting).
     */
    public static ByteList subMut(ByteList lhs, ByteList rhs) {
        // We always want to pad with one zero to the front to avoid
        // things being treated as negative and not overflowing properly.
        BigInteger bigLhs;
        if (lhs.size() > 0) {
            byte[] bytes = new byte[lhs.size() + 1];
            lhs.getElements(0, bytes, 1, lhs.size());
            bigLhs = new BigInteger(bytes);
        } else {
            bigLhs = BigInteger.ZERO;
        }
        BigInteger bigRhs;
        if (rhs.size() > 0) {
            byte[] bytes = new byte[rhs.size() + 1];
            rhs.getElements(0, bytes, 1, rhs.size());
            bigRhs = new BigInteger(bytes);
        } else {
            bigRhs = BigInteger.ZERO;
        }

        BigInteger bigDifference = bigLhs.subtract(bigRhs);
        byte[] diff = bigDifference.toByteArray();

        for (int i = 0; i < lhs.size(); i++) {
            int lhsIdx = lhs.size() - i - 1;
            int diffIdx = diff.length - i - 1;
            if (diffIdx >= 0) {
                lhs.set(lhsIdx, diff[diff.length - i - 1]);
            } else {
                // lhs is longer than the sum.
                // check if it's negative?
                if (bigDifference.compareTo(BigInteger.ZERO) >= 0) {
                    // pad with 0s
                    lhs.set(lhsIdx, (byte) 0);
                } else {
                    // pad with FFs
                    lhs.set(lhsIdx, (byte) 0xff);
                }
            }

        }
        // we will have remainder if rhs >= lhs
        if (diff.length >= lhs.size()) {
            int remainderSize = diff.length - lhs.size();
            if (remainderSize > 0) {
                return new ByteArrayList(diff, 0, remainderSize);
            } else {
                return ByteLists.EMPTY_LIST;
            }
        } else {
            // no remainder
            return ByteLists.EMPTY_LIST;
        }
    }

    /**
     * Perform lhs - rhs. Return the (underflow, sum)
     */
    public static Pair<ByteList, ByteList> sub(byte[] lhs, byte[] rhs) {
        ByteList lhsList = new ByteArrayList(lhs);
        ByteList rem = Utils.subMut(lhsList, new ByteArrayList(rhs));
        return new Pair<>(rem, lhsList);
    }

    private static final boolean DEBUG_MODE = true;
    public static void debugf(String format, Object... params) {
        if (DEBUG_MODE) {
            System.out.printf(format, params);
        }
    }

    /**
     * Flood fill from a given component and return the dumb components found.
     * Includes the thing at the given blockpos (if it's a dumb component).
     */
    // TODO: make an actual config handler.
    private static final int MAXIMUM_COMPONENT_COUNT = 1024;
    public static Set<TileDumbComputerComponent> findDumbComponents(BlockPos original, World world) {
        Set<TileDumbComputerComponent> found = new HashSet<>();
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (found.size() < MAXIMUM_COMPONENT_COUNT && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            TileEntity maybeTE = world.getTileEntity(questioning);
            if (maybeTE instanceof TileDumbComputerComponent) {
                found.add((TileDumbComputerComponent) maybeTE);
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
     * Flood fill from a given location. Pass two checkers: is this a valid block,
     * and is this my target block.
     *
     * Returns the blockpos of the first one found.
     */
    @Nullable
    public static BlockPos floodFillFor(BlockPos original, Predicate<BlockPos> isValidStep, Predicate<BlockPos> isTarget) {
        Queue<BlockPos> placesToLook = new ArrayDeque<>();
        Set<BlockPos> placesSearched = new HashSet<>();

        placesToLook.add(original);
        for (Direction d : Direction.values()) {
            placesToLook.add(original.offset(d));
        }

        while (placesSearched.size() < MAXIMUM_COMPONENT_COUNT && !placesToLook.isEmpty()) {
            BlockPos questioning = placesToLook.remove();
            if (isTarget.test(questioning)) {
                return questioning;
            }
            if (isValidStep.test(questioning)) {
                for (Direction d : Direction.values()) {
                    BlockPos nextPos = questioning.offset(d);
                    if (!placesSearched.contains(nextPos)) {
                        placesToLook.add(nextPos);
                        placesSearched.add(nextPos);
                    }
                }
            }
        }

        return null;
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

}
