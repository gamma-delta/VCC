package me.gammadelta;

import java.util.ArrayList;
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
}
