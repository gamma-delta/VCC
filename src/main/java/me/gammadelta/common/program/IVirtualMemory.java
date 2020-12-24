// thank you alwinfy for writing the start of this!
// Gonna be honest, don't really know how it works.

package me.gammadelta.common.program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public interface IVirtualMemory {
    long size();

    byte read(long address) throws Emergency;

    interface Distanced extends IVirtualMemory {
        int blockDistance();
    }

    // Immutable lookup table; relatively expensive O(n) setup, but gets very fast O(log n) retrieval
    public class LinearDispatcher implements IVirtualMemory {
        private static final IVirtualMemory[] TEMPLATE = new IVirtualMemory[0];

        private final long[] offsets;
        private final long size;
        private final IVirtualMemory[][] memories;
        private final Random random;

        public LinearDispatcher(Iterable<? extends Distanced> willReadFrom, int maxDistance, Random random) {
            @SuppressWarnings("unchecked")
            ArrayList<Distanced>[] data = (ArrayList<Distanced>[]) new ArrayList<?>[maxDistance];
            long sizeAcc = 0;
            for (Distanced d : willReadFrom) {
                sizeAcc += d.size();
                int distance = d.blockDistance();
                if (data[distance] == null) {
                    data[distance] = new ArrayList<>();
                }
                data[d.blockDistance()].add(d);
            }
            size = sizeAcc;

            memories = new IVirtualMemory[maxDistance][];
            offsets = new long[maxDistance + 1];
            offsets[0] = 0;
            for (int i = 0; i < data.length; i++) {
                offsets[i + 1] = offsets[i];
                ArrayList<Distanced> row = data[i];
                if (row == null) {
                    continue;
                }
                memories[i] = row.toArray(TEMPLATE);
                for (IVirtualMemory vm : memories[i]) {
                    offsets[i + 1] += 2 * vm.size();
                }
            }

            this.random = random;
        }

        @Override
        public byte read(long address) throws Emergency {
            if (address > size) {
                throw new Emergency();
            }

            int location = -Arrays.binarySearch(offsets, 2 * address + 1);
            IVirtualMemory[] devices = memories[location];
            address -= offsets[location];

            IVirtualMemory device = devices[random.nextInt()];
            while (device.size() >= address) {
                address -= device.size();
                device = devices[random.nextInt()];
            }

            return device.read(address);
        }

        @Override
        public long size() {
            return size;
        }
    }
}