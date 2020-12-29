package me.gammadelta.common.program;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import me.gammadelta.common.block.tile.TileChassis;
import me.gammadelta.common.block.tile.TileDumbComputerComponent;
import me.gammadelta.common.block.tile.TileMotherboard;
import me.gammadelta.common.block.tile.TileRegister;
import me.gammadelta.common.utils.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.*;

public class MotherboardRepr {

    public static int XRAM_SIZE = 256;
    public static int ROM_SIZE = 1_048_576;
    public static int RAM_SIZE = 65_536;
    public static Permissions XRAM_PERMS = Permissions.RWX;
    public static Permissions ROM_PERMS = Permissions.R;
    public static Permissions RAM_PERMS = Permissions.RW;

    // region These values are serialized back and forth.

    /**
     * UUID of this particular motherboard.
     */
    public UUID uuid;
    public static final String UUID_TAG = "vcc_uuid";

    /**
     * The contents of all memory blocks.
     * See Memory Layout on the wiki.
     */
    public byte[] memory;
    private static final String MEMORY_KEY = "memory";

    /**
     * These are in no particular order.
     * CPUs cache their distances to each one.
     */
    private EnumMap<MemoryType, ArrayList<BlockPos>> memoryLocations;
    private static final String MEMORY_LOCATIONS_KEY = "memory_locations";

    /**
     * CPUs, sorted by distance to the motherboard. Ties are in their own sub-arrays.
     * <p>
     * `[ [0], [1, 1, 1], [2, 2], [3], [4] ]`
     */
    private ArrayList<ArrayList<CPURepr>> cpus;
    private static final String CPUS_KEY = "cpus";

    /**
     * Registers, in no particular order.
     * This does not include IP/SP extenders; their reprs are stored in the CPU.
     */
    public ArrayList<RegisterRepr> registers;
    private static final String REGISTERS_KEY = "registers";

    /**
     * List of Overclock positions, in no particular order.
     */
    public ArrayList<BlockPos> overclocks;
    private static final String OVERCLOCKS_KEY = "overclocks";

    public ArrayList<BlockPos> chassises;
    private static final String CHASSISES_KEY = "chassisessisises";

    // endregion End serialized values.

    /**
     * Cached list of blocks this motherboard owns.
     * This isn't serialized because it's re-calculated.
     */
    public ArrayList<BlockPos> ownedBlocks;


    public MotherboardRepr(EnumMap<MemoryType, ArrayList<BlockPos>> memoryLocations, ArrayList<ArrayList<CPURepr>> cpus,
            ArrayList<RegisterRepr> registers, ArrayList<BlockPos> overclocks) {
        this.memoryLocations = memoryLocations;
        this.cpus = cpus;
        this.registers = registers;
        this.overclocks = overclocks;
        this.chassises = new ArrayList<>();

        this.uuid = UUID.randomUUID();

        this.initializeMemory();
        this.fillOwnedBlocks();
    }

    /**
     * Create a new empty motherboard with just enough information
     * to prevent instant NPEs.
     */
    public MotherboardRepr() {
        this.memoryLocations = new EnumMap<>(MemoryType.class);
        for (MemoryType memType : MemoryType.values()) {
            this.memoryLocations.put(memType, new ArrayList<>());
        }
        this.cpus = new ArrayList<>();
        this.registers = new ArrayList<>();
        this.overclocks = new ArrayList<>();
        this.chassises = new ArrayList<>();

        this.uuid = UUID.randomUUID();

        this.initializeMemory();
        this.fillOwnedBlocks();
    }

    /**
     * Update my connected components given the new components
     */
    public void updateComponents(TileMotherboard owner, Iterator<TileDumbComputerComponent> components) {
        // TODO: rn this is just boilerplate to prevent instant NPEs
        this.memoryLocations = new EnumMap<>(MemoryType.class);
        for (MemoryType memType : MemoryType.values()) {
            this.memoryLocations.put(memType, new ArrayList<>());
        }
        this.cpus = new ArrayList<>();
        this.registers = new ArrayList<>();
        this.overclocks = new ArrayList<>();
        this.chassises = new ArrayList<>();

        for (Iterator<TileDumbComputerComponent> it = components; it.hasNext(); ) {
            TileDumbComputerComponent component = it.next();
            component.setMotherboard(owner);

            if (component instanceof TileChassis) {
                this.chassises.add(component.getPos());
            } else if (component instanceof TileRegister) {
                // TODO: make registers combine into big registers
                this.registers.add(new RegisterRepr(new BlockPos[]{component.getPos()}));
            }
        }


        this.initializeMemory();
        this.fillOwnedBlocks();
    }

    /**
     * Deserialize a motherboard from NBT.
     */
    public MotherboardRepr(CompoundNBT tag) {
        this.memory = tag.getByteArray(MEMORY_KEY);

        CompoundNBT memoryLocsTag = tag.getCompound(MEMORY_LOCATIONS_KEY);
        this.memoryLocations = new EnumMap<>(MemoryType.class);
        for (MemoryType memType : MemoryType.values()) {
            ArrayList<BlockPos> poses = new ArrayList<>();
            ListNBT posesTag = memoryLocsTag.getList(memType.name(), Constants.NBT.TAG_COMPOUND);
            for (int c = 0; c < posesTag.size(); c++) {
                poses.add(NBTUtil.readBlockPos(posesTag.getCompound(c)));
            }
            this.memoryLocations.put(memType, poses);
        }

        ListNBT cpusTag = tag.getList(CPUS_KEY, Constants.NBT.TAG_LIST);
        this.cpus = new ArrayList<>(cpusTag.size());
        for (int c = 0; c < cpusTag.size(); c++) {
            ListNBT cpuGroupTag = cpusTag.getList(c);
            ArrayList<CPURepr> cpuGroup = new ArrayList<>(cpuGroupTag.size());
            for (int d = 0; d < cpuGroupTag.size(); d++) {
                cpuGroup.add(new CPURepr(cpuGroupTag.getCompound(d)));
            }
            this.cpus.add(cpuGroup);
        }

        ListNBT registersTag = tag.getList(REGISTERS_KEY, Constants.NBT.TAG_COMPOUND);
        this.registers = new ArrayList<>(registersTag.size());
        for (int c = 0; c < registersTag.size(); c++) {
            this.registers.add(new RegisterRepr(registersTag.getCompound(c)));
        }

        ListNBT overclocksTag = tag.getList(OVERCLOCKS_KEY, Constants.NBT.TAG_COMPOUND);
        this.overclocks = new ArrayList<>(overclocksTag.size());
        for (int c = 0; c < overclocksTag.size(); c++) {
            overclocks.add(NBTUtil.readBlockPos(overclocksTag.getCompound(c)));
        }
        ListNBT chassisTag = tag.getList(OVERCLOCKS_KEY, Constants.NBT.TAG_COMPOUND);
        this.chassises = new ArrayList<>(chassisTag.size());
        for (int c = 0; c < chassisTag.size(); c++) {
            chassises.add(NBTUtil.readBlockPos(chassisTag.getCompound(c)));
        }

        try {
            this.uuid = tag.getUniqueId(UUID_TAG);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            this.uuid = UUID.randomUUID();
        }

        this.fillOwnedBlocks();
    }

    public CompoundNBT serialize() {
        CompoundNBT tag = new CompoundNBT();

        tag.putByteArray(MEMORY_KEY, this.memory);

        CompoundNBT memLocsTag = new CompoundNBT();
        for (MemoryType memType : MemoryType.values()) {
            ArrayList<BlockPos> poses = this.memoryLocations.get(memType);
            ListNBT posesTag = new ListNBT();
            for (BlockPos pos : poses) {
                posesTag.add(NBTUtil.writeBlockPos(pos));
            }
            memLocsTag.put(memType.name(), posesTag);
        }
        tag.put(MEMORY_LOCATIONS_KEY, memLocsTag);

        ListNBT cpusTag = new ListNBT();
        for (ArrayList<CPURepr> cpuGroup : this.cpus) {
            ListNBT cpuGroupTag = new ListNBT();
            for (CPURepr cpu : cpuGroup) {
                cpuGroupTag.add(cpu.serialize());
            }
            cpusTag.add(cpuGroupTag);
        }
        tag.put(CPUS_KEY, cpusTag);

        ListNBT registersTag = new ListNBT();
        for (RegisterRepr regiGigas : this.registers) {
            registersTag.add(regiGigas.serialize());
        }
        tag.put(REGISTERS_KEY, registersTag);

        ListNBT overclocksTag = new ListNBT();
        for (BlockPos oclock : this.overclocks) {
            overclocksTag.add(NBTUtil.writeBlockPos(oclock));
        }
        tag.put(OVERCLOCKS_KEY, overclocksTag);
        ListNBT chassisTag = new ListNBT();
        for (BlockPos chassisisisisisis : this.chassises) {
            chassisTag.add(NBTUtil.writeBlockPos(chassisisisisisis));
        }
        tag.put(CHASSISES_KEY, chassisTag);

        tag.putUniqueId(UUID_TAG, this.uuid);

        return tag;
    }

    /**
     * Calculate and allocate the right size of memory, filling it with zeroes.
     */
    private void initializeMemory() {
        int memorySize = 0;
        for (Map.Entry<MemoryType, ArrayList<BlockPos>> entry : this.memoryLocations.entrySet()) {
            MemoryType mtype = entry.getKey();
            int count = entry.getValue().size();
            memorySize += mtype.storageAmount * count;
        }
        this.memory = new byte[memorySize];
    }

    /**
     * Fill in all the owned blocks.
     */
    private void fillOwnedBlocks() {
        this.ownedBlocks = new ArrayList<>();

        for (ArrayList<CPURepr> cpuGroup : this.cpus) {
            for (CPURepr cpu : cpuGroup) {
                this.ownedBlocks.add(cpu.manifestation);

                if (cpu.ipExtender != null) {
                    this.ownedBlocks.addAll(Arrays.asList(cpu.ipExtender.manifestations));
                }
                if (cpu.spExtender != null) {
                    this.ownedBlocks.addAll(Arrays.asList(cpu.spExtender.manifestations));
                }
            }
        }

        for (RegisterRepr regiIce : this.registers) {
            this.ownedBlocks.addAll(Arrays.asList(regiIce.manifestations));
        }

        this.memoryLocations.forEach((_memType, poses) -> this.ownedBlocks.addAll(poses));

        this.ownedBlocks.addAll(this.overclocks);
        this.ownedBlocks.addAll(this.chassises);
    }

    /**
     * Execute one step of the computer!
     */
    public void executeStep(World world) {
        for (ArrayList<CPURepr> cpuGroup : this.cpus) {
            int[] indices = Utils.randomIndices(cpuGroup.size(), world.getRandom());
            for (int cpuIdx : indices) {
                cpuGroup.get(cpuIdx).executeStep(this, world.rand);
            }
        }
    }

    public void executeStep(Random rand) {
        for (ArrayList<CPURepr> cpuGroup : this.cpus) {
            int[] indices = Utils.randomIndices(cpuGroup.size(), rand);
            for (int cpuIdx : indices) {
                cpuGroup.get(cpuIdx).executeStep(this, rand);
            }
        }
    }

    // region Interacting with memory

    // java bad
    private static final byte[] NEW_BYTE_ARRAY = new byte[0];

    /**
     * Access the region of memory associated with the type and that block index, and return it.
     * Effectively, this puts back the abstraction of bytes being stored across many blocks.
     */
    public ByteArrayList readRegion(MemoryType memType, int blockIdx) throws Emergency {
        int byteIdx = this.getRegionIndex(memType, blockIdx);
        // and return
        ByteArrayList bal = new ByteArrayList(memType.storageAmount);
        bal.addElements(0, this.memory, byteIdx, memType.storageAmount);
        return bal;
    }

    /**
     * Get the index in memory associated with the type and block index
     */
    public int getRegionIndex(MemoryType memType, int blockIdx) throws Emergency {
        int totalBlockCount = this.memoryLocations.get(memType).size();
        if (blockIdx >= totalBlockCount) {
            // uh-oh
            throw new Emergency();
        }
        // Calculate the byte index to start at
        int byteIdx = 0;
        for (MemoryType checkThis : MemoryType.values()) {
            if (checkThis == memType) {
                byteIdx += memType.storageAmount * blockIdx;
                break;
            }
            byteIdx += checkThis.storageAmount * this.memoryLocations.get(memType).size();
        }
        return byteIdx;
    }

    // endregion
}
