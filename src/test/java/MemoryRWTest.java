import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.program.CPURepr;
import me.gammadelta.common.program.MemoryType;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.program.Permissions;
import org.junit.jupiter.api.Test;

import java.util.*;

public class MemoryRWTest {
    @Test
    public void testPrelim() throws Exception {
        Random rand = new Random();

        EnumMap<MemoryType, ArrayList<ArrayList<Integer>>> memLocations = new EnumMap<>(MemoryType.class);
        // xrams: [0, [1, 2], 4, 3]
        memLocations.put(MemoryType.XRAM, new ArrayList<>(Arrays.asList(
                new ArrayList<>(Arrays.asList(0)),
                new ArrayList<>(Arrays.asList(1, 2)),
                new ArrayList<>(Arrays.asList(4)),
                new ArrayList<>(Arrays.asList(3))
        )));
        // fill in everything else to avoid npes
        memLocations.put(MemoryType.EXRAM, new ArrayList<>());
        memLocations.put(MemoryType.ROM, new ArrayList<>());
        memLocations.put(MemoryType.RAM, new ArrayList<>());

        // i don't initialize anything else so DON'T READ IT
        CPURepr cpu = new CPURepr(new ArrayList<>(), memLocations);

        EnumMap<MemoryType, Integer> memoryCounts = new EnumMap<>(MemoryType.class);
        // we DO have to put everything in here
        memoryCounts.put(MemoryType.XRAM, 5);
        memoryCounts.put(MemoryType.EXRAM, 0);
        memoryCounts.put(MemoryType.ROM, 0);
        memoryCounts.put(MemoryType.RAM, 0);
        MotherboardRepr motherboard = new MotherboardRepr(memoryCounts, new ArrayList<>(), new ArrayList<>(), rand);

        // Fill up the memory.
        for (int idx = 0; idx < motherboard.memory.length; idx++) {
            motherboard.memory[idx] = (byte) (idx / 256); // 000 ... 111 ... 222 ... etc
        }

        // And finally start reading.
        for (int idx = 0; idx < 256 * 5; idx += 256) {
            System.out.printf("reading from %s:\n", idx);
            for (int count = 0; count < 4; count++) {
                byte[] read = cpu.read(idx, 10, Permissions.R, motherboard, rand);
                System.out.printf("- #%d:  %s\n", count + 1, Arrays.toString(read));
            }
        }

        // And read on an offset
        for (int idx = 256; idx < 256 * 4; idx += 256) {
            int realIdx = idx - 5;
            System.out.printf("reading from %s:\n", realIdx);
            for (int count = 0; count < 4; count++) {
                byte[] read = cpu.read(realIdx, 10, Permissions.R, motherboard, rand);
                System.out.printf("- #%d:  %s\n", count + 1, Arrays.toString(read));
            }
        }

        // And finally read a whole lot
        // should be 1/2, then 4, then 3?
        {
            byte[] read = cpu.read(760, 300, Permissions.R, motherboard, rand);
            System.out.println(Arrays.toString(read));
        }
    }
}
