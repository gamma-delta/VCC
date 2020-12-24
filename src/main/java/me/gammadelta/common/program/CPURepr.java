package me.gammadelta.common.program;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import me.gammadelta.Utils;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

/**
 * Representation of a CPU.
 */
public class CPURepr {
    public byte FLAGS = 0;
    public long IP = 0;
    public long SP = 0;
    @Nullable
    public RegisterRepr ipExtender;
    @Nullable
    public RegisterRepr spExtender;

    /** Where the CPU block that pretends to be this is */
    public BlockPos manifestation;

    /**
     * Cached indexes of registers this CPU can see.
     * The indexes refer to the array of registers in the motherboard.
     * Closest -> farthest. Equidistant ones are in subarrays.
     */
    public ArrayList<ArrayList<Integer>> registers;
    /** Cached total number of registers */
    public int registerCount;

    /**
     * Cached indexes of datafaces this CPU can see.
     * The indexes refer to the array of datafaces in the motherboard.
     */
    public ArrayList<ArrayList<Integer>> datafaces;
    public int datafaceCount;

    /**
     * Each array stores the indices that this CPU thinks the given memory is at.
     */
    public EnumMap<MemoryType, ArrayList<ArrayList<Integer>>> memoryLocations;
    public EnumMap<MemoryType, Integer> memoryCounts;

    public CPURepr(@Nullable RegisterRepr ipExtender, @Nullable RegisterRepr spExtender, BlockPos manifestation, ArrayList<ArrayList<Integer>> registers, ArrayList<ArrayList<Integer>> datafaces, EnumMap<MemoryType, ArrayList<ArrayList<Integer>>> memoryLocations) {
        this.manifestation = manifestation;
        this.ipExtender = ipExtender;
        this.spExtender = spExtender;

        this.registers = registers;
        this.registerCount = this.registers.stream().mapToInt(ArrayList::size).sum();
        this.datafaces = datafaces;
        this.datafaceCount = this.datafaces.stream().mapToInt(ArrayList::size).sum();
        this.memoryLocations = memoryLocations;
        this.memoryCounts = new EnumMap<>(MemoryType.class);
        memoryLocations.forEach(((memType, groups) -> {
            int memCount = groups.stream().mapToInt(ArrayList::size).sum();
            this.memoryCounts.put(memType, memCount);
        }));
    }

    /**
     * Cut down constructor mostly for testing purposes.
     * @param registers
     */
    public CPURepr(ArrayList<ArrayList<Integer>> registers, EnumMap<MemoryType, ArrayList<ArrayList<Integer>>> memoryLocations) {
        this(null, null, new BlockPos(-1, -1, -1), registers, new ArrayList<>(), memoryLocations);
    }

    // Turn a raw memory index into a MemoryLocation
    private BlockwiseMemoryLocation idxToLocation(long idx) throws Emergency {
        long bytesLeftover = idx;
        long remainingIdx = -1;
        MemoryType readFrom = null;
        for (MemoryType checkThis : MemoryType.values()) {
            long bytesReduced = bytesLeftover - this.memoryCounts.get(checkThis) * checkThis.storageAmount;
            if (bytesReduced < 0) {
                // this means reading from the end of this section would bring it below 0
                // so our answer is in there!
                readFrom = checkThis;
                remainingIdx = bytesLeftover;
                break;
            }
            bytesLeftover = bytesReduced;
        }
        if (readFrom == null) {
            // we never found the spot ;-;
            throw new Emergency();
        }

        int blockIdx = (int) (remainingIdx / readFrom.storageAmount);

        return new BlockwiseMemoryLocation(idx, blockIdx, readFrom);
    }

    // Read bytes from a memory location.
    public byte[] read(long readIdx, int readThisMuch, Permissions perms, MotherboardRepr mother, Random rand) throws Emergency {
        ByteArrayList gotRead = new ByteArrayList(readThisMuch);

        // We might need to read over multiple
        long currentIdx = readIdx;
        for (BlockwiseMemoryLocation bml = this.idxToLocation(currentIdx);
             gotRead.size() < readThisMuch; bml = this.idxToLocation(currentIdx)) {
            if (!bml.memType.perms.satisfiedBy(perms)) {
                // uh oh we're trying to do an invalid operation
                throw new Emergency();
            }
            // index of this memory block wrt the motherboard's master copy
            int mothersMemIdx = Utils.getUncertainNested(this.memoryLocations.get(bml.memType), bml.blockIdx, rand);
            // read the actual memory
            ByteArrayList readRegion = mother.readRegion(bml.memType, mothersMemIdx);
            // Slice apart the read data
            int readStart;
            int readEnd;

            if (gotRead.size() == 0) {
                // This is our first time reading
                readStart = (int) (readIdx % bml.memType.storageAmount);
                readEnd = Integer.min(readStart + readThisMuch, readRegion.size());
            } else {
                // not our first time
                readStart = 0;
                int bytesLeftToRead = readThisMuch - gotRead.size();
                readEnd = Integer.min(bytesLeftToRead, readRegion.size());
            }
            ByteList sliced = readRegion.subList(readStart, readEnd);
            gotRead.addAll(sliced);
            currentIdx += sliced.size();

        }

        assert gotRead.size() == readThisMuch;
        return gotRead.toArray(new byte[0]);
    }


    // region Dealing with interpreting bytecode

    public void executeStep(MotherboardRepr mother, World world) {
        // TODO
    }


    // endregion
}
