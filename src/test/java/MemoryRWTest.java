import com.google.common.primitives.Bytes;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.common.utils.Utils;
import me.gammadelta.common.program.CPURepr;
import me.gammadelta.common.program.MemoryType;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.program.Permissions;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;

public class MemoryRWTest {
    @Test
    public void testReadPrelim() throws Exception {
        Random rand = new Random();

        EnumMap<MemoryType, ArrayList<IntList>> memLocations = new EnumMap<>(MemoryType.class);
        // xrams: [0, [1, 2], 4, 3]
        memLocations.put(MemoryType.XRAM, new ArrayList<>(Arrays.asList(
                new IntArrayList(Arrays.asList(0)),
                new IntArrayList(Arrays.asList(1, 2)),
                new IntArrayList(Arrays.asList(4)),
                new IntArrayList(Arrays.asList(3))
        )));
        // fill in everything else to avoid npes
        memLocations.put(MemoryType.EXRAM, new ArrayList<>());
        memLocations.put(MemoryType.ROM, new ArrayList<>());
        memLocations.put(MemoryType.RAM, new ArrayList<>());

        // i don't initialize anything else so DON'T READ IT
        CPURepr cpu = new CPURepr(new ArrayList<>(), memLocations);

        EnumMap<MemoryType, ArrayList<BlockPos>> memoryCounts = new EnumMap<>(MemoryType.class);
        // we DO have to put everything in here
        memoryCounts.put(MemoryType.XRAM, new ArrayList<>(
                Arrays.asList(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO)));
        memoryCounts.put(MemoryType.EXRAM, new ArrayList<>());
        memoryCounts.put(MemoryType.ROM, new ArrayList<>());
        memoryCounts.put(MemoryType.RAM, new ArrayList<>());
        MotherboardRepr motherboard = new MotherboardRepr(memoryCounts, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());

        // Fill up the memory.
        for (int idx = 0; idx < motherboard.memory.length; idx++) {
            motherboard.memory[idx] = (byte) (idx / 256); // 000 ... 111 ... 222 ... etc
        }

        // And finally start reading.
        for (int idx = 0; idx < 256 * 5; idx += 256) {
            System.out.printf("reading from %s:\n", idx);
            for (int count = 0; count < 4; count++) {
                ByteList read = cpu.read(idx, 10, Permissions.R, motherboard, rand);
                System.out.printf("- #%d:  %s\n", count + 1, Arrays.toString(read.toArray()));
            }
        }

        // And read on an offset
        for (int idx = 256; idx < 256 * 4; idx += 256) {
            int realIdx = idx - 5;
            System.out.printf("reading from %s:\n", realIdx);
            for (int count = 0; count < 4; count++) {
                ByteList read = cpu.read(realIdx, 10, Permissions.R, motherboard, rand);
                System.out.printf("- #%d:  %s\n", count + 1, Arrays.toString(read.toArray()));
            }
        }

        // And finally read a whole lot
        // should be 1/2, then 4, then 3?
        {
            ByteList read = cpu.read(760, 300, Permissions.R, motherboard, rand);
            System.out.println(Arrays.toString(read.toArray()));
        }
    }

    @Test
    public void testWritePrelim() throws Exception {
        Random rand = new Random();

        EnumMap<MemoryType, ArrayList<IntList>> memLocations = new EnumMap<>(MemoryType.class);
        // xrams: [0, [1, 2], 4, 3]
        memLocations.put(MemoryType.XRAM, new ArrayList<>(Arrays.asList(
                new IntArrayList(Arrays.asList(0)),
                new IntArrayList(Arrays.asList(1, 2)),
                new IntArrayList(Arrays.asList(4)),
                new IntArrayList(Arrays.asList(3))
        )));
        // fill in everything else to avoid npes
        memLocations.put(MemoryType.EXRAM, new ArrayList<>());
        memLocations.put(MemoryType.ROM, new ArrayList<>());
        memLocations.put(MemoryType.RAM, new ArrayList<>());

        // i don't initialize anything else so DON'T READ IT
        CPURepr cpu = new CPURepr(new ArrayList<>(), memLocations);

        EnumMap<MemoryType, ArrayList<BlockPos>> memoryCounts = new EnumMap<>(MemoryType.class);
        // we DO have to put everything in here
        memoryCounts.put(MemoryType.XRAM, new ArrayList<>(
                Arrays.asList(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO)));
        memoryCounts.put(MemoryType.EXRAM, new ArrayList<>());
        memoryCounts.put(MemoryType.ROM, new ArrayList<>());
        memoryCounts.put(MemoryType.RAM, new ArrayList<>());
        MotherboardRepr motherboard = new MotherboardRepr(memoryCounts, new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>());

        // Fill up the memory.
        for (int idx = 0; idx < motherboard.memory.length; idx++) {
            motherboard.memory[idx] = (byte) (idx / 256); // 000 ... 111 ... 222 ... etc
        }

        // Write things
        // Write 0x10 to places in 0
        for (int time = 0; time < 8; time++) {
            int idx = time * 16;
            cpu.write(idx, new ByteArrayList(new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10}),
                    Permissions.RW, motherboard, rand);
        }

        // Write across the 4-3 memory boundary
        {
            cpu.write(256 * 4 - 8,
                    new ByteArrayList(
                            new byte[]{0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20}),
                    Permissions.RW, motherboard, rand);
        }

        // Scatter bytes around the 1-2 region
        for (int time = 0; time < 256; time++) {
            int idx = 256 + time;
            // -1 == 0xff
            cpu.write(idx, new ByteArrayList(new byte[]{-1}), Permissions.RW, motherboard, rand);
        }

        System.out.println(Utils.hexdump(Bytes.asList(motherboard.memory)));
    }
}
