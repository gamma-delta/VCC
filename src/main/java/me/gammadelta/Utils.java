package me.gammadelta;

import com.google.common.base.Splitter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class Utils {
    /**
     * Generate a random list of numbers from [0, n), each appearing exactly once.
     */
    public static int[] randomIndices(int count, Random rand) {
        int[] out = new int[count];
        // Fill the array with the numbers in sequence
        for (int n = 0; n < count; n ++) {
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

    public static String hexdump(List<Byte> bytes) {
        StringBuilder bob = new StringBuilder();
        int lineIdx = 0;
        for (int idx = 0; idx < bytes.size(); idx++) {
            lineIdx = idx % 16;
            if (lineIdx == 0) {
                // start of line
                String region = String.format("%08x  ", idx);
                bob.append(region);
            }
            bob.append(String.format("%02x ", bytes.get(idx)));
            if (lineIdx == 7) {
                // put spacer between 8 bytes
                bob.append(' ');
            } else if (lineIdx == 15) {
                // end of line!
                bob.append('\n');
            }
        }
        // Append the total length
        if (lineIdx != 15) {
            // newline if we weren't perfectly on the money
            bob.append('\n');
        }
        bob.append(String.format("%08x", bytes.size()));

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
}
