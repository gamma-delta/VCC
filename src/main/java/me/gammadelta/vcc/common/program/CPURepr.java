package me.gammadelta.vcc.common.program;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.vcc.common.program.compilation.Opcode;
import me.gammadelta.vcc.common.utils.BinaryUtils;
import me.gammadelta.vcc.common.utils.Utils;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntArrayNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

import static me.gammadelta.vcc.common.program.compilation.Opcode.ILLEGAL;
import static me.gammadelta.vcc.common.program.compilation.Opcode.OPCODES_TO_BYTECODE;

/**
 * Representation of a CPU.
 */
public class CPURepr {
    static byte UNDERFLOW_BIT = 0b00000001;
    static byte UNDERFLOW_MASK = (byte) ~UNDERFLOW_BIT;
    static byte OVERFLOW_BIT = 0b00000010;
    static byte OVERFLOW_MASK = (byte) ~OVERFLOW_BIT;
    static byte EXPAND_BIT = 0b00000100;
    static byte EXPAND_MASK = (byte) ~EXPAND_BIT;
    static byte SQUISH_BIT = 0b00001000;
    static byte SQUISH_MASK = (byte) ~SQUISH_BIT;

    // this must be an array because we need to pass it by reference
    // or, yknow pass the reference by value
    // in conclusion, Java bad
    public byte[] FLAGS;
    private static final String FLAGS_TAG = "flags";
    public byte[] IP;
    private static final String IP_TAG = "IP";
    public byte[] SP;
    private static final String SP_TAG = "SP";

    // These references really only exist to prevent them from being dropped.
    // Most things should be done through the IP and SP variables.
    // In particular, the actual value is not stored inside the RegisterRepr,
    // but inside IP and SP.
    @Nullable
    public RegisterRepr ipExtender;
    private static final String IP_EXTENDER_TAG = "IP_extender";
    @Nullable
    public RegisterRepr spExtender;
    private static final String SP_EXTENDER_TAG = "SP_extender";

    // Used to make working with the IP in the middle of a operation not difficult
    // It's set to the IP at the beginning of every step and written back.
    private long currentIP;

    /**
     * Where the CPU block that pretends to be this is
     */
    public BlockPos manifestation;
    private static final String MANIFESTATION_TAG = "manifestation";

    /**
     * Cached indexes of registers this CPU can see.
     * The indexes refer to the array of registers in the motherboard.
     * Closest -> farthest. Equidistant ones are in subarrays.
     */
    public ArrayList<IntList> registers;
    private static final String REGISTERS_TAG = "register_idxes";
    public int registerCount;

    /**
     * Cached indexes of datafaces this CPU can see.
     * The indexes refer to the array of datafaces in the motherboard.
     */
    public ArrayList<IntList> datafaces;
    private static final String DATAFACES_TAG = "dataface_idxes";
    public int datafaceCount;

    /**
     * Each array stores the indices that this CPU thinks the given memory is at.
     */
    public EnumMap<MemoryType, ArrayList<IntList>> memoryLocations;
    private static final String MEMORY_LOCATIONS_TAG = "memory_locations";
    public EnumMap<MemoryType, Integer> memoryCounts;
    public EnumMap<MemoryType, Long> memoryStarts;

    public CPURepr(@Nullable RegisterRepr ipExtender, @Nullable RegisterRepr spExtender, BlockPos manifestation,
            ArrayList<IntList> registers, ArrayList<IntList> datafaces,
            EnumMap<MemoryType, ArrayList<IntList>> memoryLocations) {
        this.manifestation = manifestation;
        this.ipExtender = ipExtender;
        this.spExtender = spExtender;

        this.registers = registers;
        this.datafaces = datafaces;
        this.memoryLocations = memoryLocations;

        if (this.ipExtender != null) {
            this.IP = new byte[this.ipExtender.getByteCount() + 1];
        } else {
            this.IP = new byte[1];
        }
        if (this.spExtender != null) {
            this.SP = new byte[this.spExtender.getByteCount() + 1];
        } else {
            this.SP = new byte[1];
        }
        this.FLAGS = new byte[1];

        initializeIdxes();
    }

    /**
     * Cut down constructor mostly for testing purposes.
     */
    public CPURepr(ArrayList<IntList> registers,
            EnumMap<MemoryType, ArrayList<IntList>> memoryLocations) {
        this(null, null, new BlockPos(-1, -1, -1), registers, new ArrayList<>(), memoryLocations);
    }

    /**
     * Deserialize a CPURepr from NBT.
     */
    public CPURepr(CompoundNBT tag) {
        this.manifestation = NBTUtil.readBlockPos(tag.getCompound(MANIFESTATION_TAG));
        CompoundNBT ipTag = tag.getCompound(IP_EXTENDER_TAG);
        if (!ipTag.isEmpty()) {
            this.ipExtender = new RegisterRepr(ipTag);
        }
        CompoundNBT spTag = tag.getCompound(SP_EXTENDER_TAG);
        if (!spTag.isEmpty()) {
            this.spExtender = new RegisterRepr(spTag);
        }

        this.registers = new ArrayList<>();
        ListNBT regis = tag.getList(REGISTERS_TAG, Constants.NBT.TAG_INT_ARRAY);
        for (int c = 0; c < regis.size(); c++) {
            int[] group = regis.getIntArray(c);
            this.registers.add(new IntArrayList(group));
        }
        this.datafaces = new ArrayList<>();
        ListNBT dfaces = tag.getList(DATAFACES_TAG, Constants.NBT.TAG_INT_ARRAY);
        for (int c = 0; c < regis.size(); c++) {
            int[] group = dfaces.getIntArray(c);
            this.datafaces.add(new IntArrayList(group));
        }

        this.memoryLocations = new EnumMap<>(MemoryType.class);
        CompoundNBT memLocs = tag.getCompound(MEMORY_LOCATIONS_TAG);
        for (MemoryType expected : MemoryType.values()) {
            ListNBT locationsTag = memLocs.getList(expected.name(), Constants.NBT.TAG_INT_ARRAY);
            ArrayList<IntList> locations = new ArrayList<>(locationsTag.size());
            for (int i = 0; i < locationsTag.size(); i++) {
                int[] groupTag = locationsTag.getIntArray(i);
                locations.add(new IntArrayList(groupTag));
            }
            this.memoryLocations.put(expected, locations);
        }

        this.IP = tag.getByteArray(IP_TAG);
        this.SP = tag.getByteArray(SP_TAG);
        this.FLAGS = tag.getByteArray(FLAGS_TAG);

        this.initializeIdxes();
    }

    public CompoundNBT serialize() {
        CompoundNBT tag = new CompoundNBT();

        tag.put(MANIFESTATION_TAG, NBTUtil.writeBlockPos(this.manifestation));

        if (this.ipExtender != null) {
            tag.put(IP_EXTENDER_TAG, this.ipExtender.serialize());
        }
        if (this.spExtender != null) {
            tag.put(SP_EXTENDER_TAG, this.spExtender.serialize());
        }

        ListNBT regisTag = new ListNBT();
        for (IntList regiGroup : this.registers) {
            regisTag.add(new IntArrayNBT(regiGroup.toIntArray()));
        }
        tag.put(REGISTERS_TAG, regisTag);
        ListNBT dfacesTag = new ListNBT();
        for (IntList dfaceGroup : this.datafaces) {
            dfacesTag.add(new IntArrayNBT(dfaceGroup.toIntArray()));
        }
        tag.put(DATAFACES_TAG, dfacesTag);

        CompoundNBT memLocs = new CompoundNBT();
        this.memoryLocations.forEach((memType, locations) -> {
            ListNBT locationsTag = new ListNBT();
            for (IntList group : locations) {
                locationsTag.add(new IntArrayNBT(group.toIntArray()));
            }
            memLocs.put(memType.name(), locationsTag);
        });
        tag.put(MEMORY_LOCATIONS_TAG, memLocs);

        tag.putByteArray(IP_TAG, this.IP);
        tag.putByteArray(SP_TAG, this.SP);
        tag.putByteArray(FLAGS_TAG, this.FLAGS);

        return tag;
    }

    /**
     * Count the number of registers and datafaces, calculate where and how much memory there is.
     */
    private void initializeIdxes() {
        this.registerCount = this.registers.stream().mapToInt(IntList::size).sum();
        this.datafaceCount = this.datafaces.stream().mapToInt(IntList::size).sum();

        this.memoryCounts = new EnumMap<>(MemoryType.class);
        this.memoryStarts = new EnumMap<>(MemoryType.class);
        long currentMemIdx = 0;
        for (MemoryType memType : MemoryType.values()) {
            ArrayList<IntList> groups = this.memoryLocations.get(memType);
            int memCount = groups.stream().mapToInt(IntList::size).sum();
            this.memoryCounts.put(memType, memCount);
            this.memoryStarts.put(memType, currentMemIdx);
            currentMemIdx += memCount * memType.storageAmount;
        }
    }

    // Turn a raw memory index into a MemoryLocation
    private BlockwiseMemoryLocation idxToLocation(long idx) throws Emergency {
        long bytesLeftover = idx;
        long remainingIdx = -1;
        MemoryType readFrom = null;
        for (MemoryType checkThis : MemoryType.values()) {
            long bytesReduced = bytesLeftover - this.memoryCounts.get(checkThis) * checkThis.storageAmount;
            if (bytesReduced <= 0) {
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
    public ByteList read(long readIdx, int readThisMuch, Permissions perms, MotherboardRepr mother,
            Random rand) throws Emergency {
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
            int mothersMemIdx = Utils.getUncertainNestedInt(this.memoryLocations.get(bml.memType), bml.blockIdx, rand);
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
        return gotRead;
    }

    // Write bytes to a memory location
    public void write(long writeIdx, ByteList toWrite, Permissions perms, MotherboardRepr mother,
            Random rand) throws Emergency {
        long bytesWritten = 0;
        long currentIdx = writeIdx;
        for (BlockwiseMemoryLocation bml = this.idxToLocation(currentIdx);
                bytesWritten < toWrite.size(); bml = this.idxToLocation(currentIdx)) {
            if (!bml.memType.perms.satisfiedBy(perms)) {
                // uh oh we're trying to do an invalid operation
                throw new Emergency();
            }
            /*
                [........] [........] [........] [........] [........]
                    ^,,,,   ,,,,,,,,   ,,,,^
             */
            // index of the block we are reading from
            int motherBlockIdx = Utils.getUncertainNestedInt(this.memoryLocations.get(bml.memType), bml.blockIdx, rand);
            // byte idx where the region starts
            int motherRegionStartIdx = mother.getRegionIndex(bml.memType, motherBlockIdx);

            // index to start writing in the motherboard's memory
            int writeStart;
            int bytesToWriteThisCycle;
            if (bytesWritten == 0) {
                // the start of writing; we need to put it in the middle of that region
                writeStart = (int) (motherRegionStartIdx + (writeIdx % bml.memType.storageAmount));
                bytesToWriteThisCycle = Math.min(bml.memType.storageAmount - (writeStart - motherRegionStartIdx),
                        toWrite.size());
            } else {
                // the middle of writing; it's just the region start
                writeStart = motherRegionStartIdx;
                bytesToWriteThisCycle = Math.min((int) (toWrite.size() - bytesWritten), bml.memType.storageAmount);
            }
            for (int i = 0; i < bytesToWriteThisCycle; i++) {
                int fromIdx = (int) bytesWritten + i;
                int toIdx = writeStart + i;
                mother.memory[toIdx] = toWrite.getByte(fromIdx);
            }
            bytesWritten += bytesToWriteThisCycle;
            currentIdx += bytesToWriteThisCycle;
        }
        assert bytesWritten == toWrite.size();
    }


    // region Dealing with interpreting bytecode

    public enum StepResult {
        NORMAL,
        UNCAUGHT_EXCEPTION,
        CAUGHT_EXCEPTION
    }

    public StepResult executeStep(MotherboardRepr mother, Random rand) {
        this.currentIP = BinaryUtils.toLong(this.IP);
        try {
            Opcode opcode = this.readOpcode(mother, rand);

            switch (opcode) {
                case NOP:
                    // boy, that was easy to implement
                    break;

                case MOV:
                    this.opcMOV(mother, rand);
                    break;
                case READ:
                    this.opcREAD(mother, rand);
                    break;
                case WRITE:
                    this.opcWRITE(mother, rand);
                    break;
                case COPY:
                    this.opcCOPY(mother, rand);
                    break;
                case PUSH:
                    this.opcPUSH(mother, rand);
                    break;
                case POP:
                    this.opcPOP(mother, rand);
                    break;
                case CALL:
                    this.opcCALL(mother, rand);
                    break;
                case RET:
                    this.opcRET(mother, rand);
                    break;

                case ADD:
                    this.opcADD(mother, rand);
                    break;
                case SUB:
                    this.opcSUB(mother, rand);
                    break;
                case INC:
                    this.opcINC(mother, rand);
                    break;
                case DEC:
                    this.opcDEC(mother, rand);
                    break;
                case AND:
                    this.opcAND(mother, rand);
                    break;
                case SHL:
                    this.opcSHL(mother, rand);
                    break;

                case JMP:
                    this.opcJMP(mother, rand);
                    break;
                case JZ:
                    this.opcJZ(mother, rand);
                    break;
                case JNZ:
                    this.opcJNZ(mother, rand);
                    break;
                case JGZ:
                    this.opcJGZ(mother, rand);
                    break;
                case JLZ:
                    this.opcJLZ(mother, rand);
                    break;

                case PRINT:
                    this.opcPRINT(mother, rand);
                    break;
                case DEBUG:
                    this.opcDEBUG(mother, rand);
                    break;
                case EMERGENCY:
                    throw new Emergency();

                default:
                    throw new NotImplementedException(
                            String.format("%s is not implemented (at idx 0x%x)", opcode, this.currentIP - 1));
            }

            // Put currentIP back into the IP to make our lives easier
            // Discard the squish/stretch, we don't need it
            // TODO: write to move flags when incrementing the IP?
            BinaryUtils.writeLong(this.currentIP, this.IP);
            return StepResult.NORMAL;
        } catch (Emergency e) {
            // oops. go to exram, go directly to exram
            if (this.memoryCounts.get(MemoryType.EXRAM) == 0) {
                // actually go to the start
                BinaryUtils.writeLong(0, this.IP);
                return StepResult.UNCAUGHT_EXCEPTION;
            } else {
                // go to (a randomly selected) exram
                int motherExramIdx = Utils.getUncertainNestedInt(this.memoryLocations.get(MemoryType.EXRAM), 0, rand);
                try {
                    int exramByteIdx = mother.getRegionIndex(MemoryType.EXRAM, motherExramIdx);
                    BinaryUtils.writeLong(exramByteIdx, this.IP);
                    return StepResult.CAUGHT_EXCEPTION;
                } catch (Emergency e2) {
                    // just set it to zero, whatever
                    BinaryUtils.writeLong(0, this.IP);
                    return StepResult.UNCAUGHT_EXCEPTION;
                }
            }
        }
    }


    // region Opcode implementation

    private void opcMOV(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList source = this.readIV(this.currentIP, true, mother, rand);
        this.writeToRegister(this.currentIP, true, source, mother, rand);
    }

    private void opcREAD(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList lenList = this.readIV(this.currentIP, true, mother, rand);
        long len = BinaryUtils.toLong(lenList);
        ByteList readBytes = this.readExternal(this.currentIP, true, (int) len, mother, rand);
        this.writeToRegister(this.currentIP, true, readBytes, mother, rand);
    }

    private void opcWRITE(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList toWrite = this.readIV(this.currentIP, true, mother, rand);
        this.writeToExternal(this.currentIP, true, toWrite, mother, rand);
    }

    private void opcCOPY(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList lenList = this.readIV(this.currentIP, true, mother, rand);
        long len = BinaryUtils.toLong(lenList);
        ByteList readBytes = this.readExternal(this.currentIP, true, (int) len, mother, rand);
        this.writeToExternal(this.currentIP, true, readBytes, mother, rand);
    }

    private void opcPUSH(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList toPush = this.readIV(this.currentIP, true, mother, rand);
        this.writeToStackvalue(toPush, 0, mother, rand);
        // Add the size of the push to SP
        byte[] rhs = new byte[toPush.size() / 256 + 1];
        BinaryUtils.writeLong(toPush.size(), rhs);
        Pair<ByteList, ByteList> res = BinaryUtils.add(this.SP, rhs);
        ByteList newSP = res.getSecond();

        for (int i = 0; i < this.SP.length; i++) {
            int spIdx = this.SP.length - i - 1;
            int newIdx = newSP.size() - i - 1;
            if (newIdx >= 0) {
                this.SP[spIdx] = newSP.getByte(newIdx);
            } else {
                this.SP[spIdx] = 0;
            }
        }
    }

    private void opcPOP(MotherboardRepr mother, Random rand) throws Emergency {
        long regiIdx = this.currentIP;
        Pair<Integer, RegisterRepr> regiInfo = this.fetchRegisterInfo(this.currentIP, true, mother, rand);
        if (regiInfo == null) {
            throw new Emergency();
        }
        int regiSize;
        if (regiInfo.getFirst() >= 0) {
            regiSize = regiInfo.getSecond().getByteCount();
        } else {
            // special register
            int idx = regiInfo.getFirst();
            if (idx == -1 || idx == -4) {
                // nil or flags
                regiSize = 1;
            } else if (idx == -2) {
                regiSize = this.IP.length;
            } else {
                regiSize = this.SP.length;
            }
        }

        // Subtract the size of the push from SP
        byte[] rhs = new byte[regiSize / 256 + 1];
        BinaryUtils.writeLong(regiSize, rhs);
        Pair<ByteList, ByteList> res = BinaryUtils.sub(this.SP, rhs);
        ByteList newSP = res.getSecond();

        for (int i = 0; i < this.SP.length; i++) {
            int spIdx = this.SP.length - i - 1;
            int newIdx = newSP.size() - i - 1;
            if (newIdx >= 0) {
                this.SP[spIdx] = newSP.getByte(newIdx);
            } else {
                this.SP[spIdx] = 0;
            }
        }

        // And read off the value
        ByteList stackRead = this.readFromStack(0, regiSize, mother, rand);
        // and assign it
        this.writeToRegister(regiIdx, false, stackRead, mother, rand);
    }

    private void opcCALL(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList jumpLoc = this.readIV(this.currentIP, true, mother, rand);

        byte[] toPushArray = new byte[this.IP.length];
        // Use `currentIP` because it now points to the *next* instruction
        // This prevents RET-ing right back to the call
        // who would do something so stupid like that? not me...
        BinaryUtils.writeLong(this.currentIP, toPushArray);
        ByteList toPush = new ByteArrayList(toPushArray);
        this.writeToStackvalue(toPush, 0, mother, rand);
        // Add the size of the push to SP
        byte[] rhs = new byte[toPush.size() / 256 + 1];
        BinaryUtils.writeLong(toPush.size(), rhs);
        Pair<ByteList, ByteList> res = BinaryUtils.add(this.SP, rhs);
        ByteList newSP = res.getSecond();

        for (int i = 0; i < this.SP.length; i++) {
            int spIdx = this.SP.length - i - 1;
            int newIdx = newSP.size() - i - 1;
            if (newIdx >= 0) {
                this.SP[spIdx] = newSP.getByte(newIdx);
            } else {
                this.SP[spIdx] = 0;
            }
        }

        // Jump to the read location
        this.currentIP = BinaryUtils.toLong(jumpLoc);
    }

    private void opcRET(MotherboardRepr mother, Random rand) throws Emergency {
        int regiSize = this.IP.length;

        // Subtract the size of the push from SP
        byte[] rhs = new byte[regiSize / 256 + 1];
        BinaryUtils.writeLong(regiSize, rhs);
        Pair<ByteList, ByteList> res = BinaryUtils.sub(this.SP, rhs);
        ByteList newSP = res.getSecond();

        for (int i = 0; i < this.SP.length; i++) {
            int spIdx = this.SP.length - i - 1;
            int newIdx = newSP.size() - i - 1;
            if (newIdx >= 0) {
                this.SP[spIdx] = newSP.getByte(newIdx);
            } else {
                this.SP[spIdx] = 0;
            }
        }

        // And read off the value
        ByteList stackRead = this.readFromStack(0, regiSize, mother, rand);
        // and assign it
        this.currentIP = BinaryUtils.toLong(stackRead);
    }

    private void opcADD(MotherboardRepr mother, Random rand) throws Emergency {
        long srcIdx = this.currentIP;
        ByteList lhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList rhs = this.readIV(this.currentIP, true, mother, rand);
        ByteList overflow = BinaryUtils.addMut(lhs, rhs);
        this.writeToRegister(srcIdx, false, lhs, mother, rand);

        if (overflow.size() > 0) {
            this.FLAGS[0] |= OVERFLOW_BIT;
        } else {
            this.FLAGS[0] &= OVERFLOW_MASK;
        }
        // no overflow for you
        this.FLAGS[0] &= UNDERFLOW_MASK;
    }

    private void opcSUB(MotherboardRepr mother, Random rand) throws Emergency {
        long srcIdx = this.currentIP;
        ByteList lhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList rhs = this.readIV(this.currentIP, true, mother, rand);
        ByteList underflow = BinaryUtils.subMut(lhs, rhs);
        this.writeToRegister(srcIdx, false, lhs, mother, rand);

        // Underflow?
        // TODO: underflow doesn't work properly
        if (underflow.size() > 0) {
            this.FLAGS[0] |= UNDERFLOW_BIT;
        } else {
            this.FLAGS[0] &= UNDERFLOW_MASK;
        }
        // no overflow for you
        this.FLAGS[0] &= OVERFLOW_MASK;
    }

    private void opcINC(MotherboardRepr mother, Random rand) throws Emergency {
        long regiIdx = this.currentIP;
        ByteList val = this.readRegister(this.currentIP, true, mother, rand);
        ByteList remainder = BinaryUtils.addMut(val, ByteLists.singleton((byte) 1));
        this.writeToRegister(regiIdx, false, val, mother, rand);
        // Overflow flag?
        if (remainder.size() > 0) {
            // oof
            this.FLAGS[0] |= OVERFLOW_BIT;
        } else {
            this.FLAGS[0] &= OVERFLOW_MASK;
        }
        // always clear the underflow bit
        this.FLAGS[0] &= UNDERFLOW_MASK;
    }

    private void opcDEC(MotherboardRepr mother, Random rand) throws Emergency {
        long regiIdx = this.currentIP;
        ByteList val = this.readRegister(this.currentIP, true, mother, rand);
        ByteList remainder = BinaryUtils.subMut(val, ByteLists.singleton((byte) 1));
        this.writeToRegister(regiIdx, false, val, mother, rand);
        // Overflow flag?
        if (remainder.size() > 0) {
            // oof
            this.FLAGS[0] |= UNDERFLOW_BIT;
        } else {
            this.FLAGS[0] &= UNDERFLOW_MASK;
        }
        // always clear the underflow bit
        this.FLAGS[0] &= OVERFLOW_MASK;
    }

    private void opcAND(MotherboardRepr mother, Random rand) throws Emergency {
        // copy the original register idx
        long regiIdx = this.currentIP;
        ByteList lhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList rhs = this.readIV(this.currentIP, true, mother, rand);
        // apply AND
        for (int i = 0; i < Math.min(lhs.size(), rhs.size()); i++) {
            int lhsIdx = lhs.size() - i - 1;
            byte lhsByte = lhs.getByte(lhsIdx);
            lhsByte &= rhs.getByte(rhs.size() - i - 1);
            lhs.set(lhsIdx, lhsByte);
        }
        // Write it back to the original register
        this.writeToRegister(regiIdx, false, lhs, mother, rand);
    }

    private void opcSHL(MotherboardRepr mother, Random rand) throws Emergency {
        // copy the original register idx
        long regiIdx = this.currentIP;
        ByteList lhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList shlAmtList = this.readIV(this.currentIP, true, mother, rand);
        long shlAmt = BinaryUtils.toLong(shlAmtList);
        long byteShlAmt = shlAmt / 8;

        long overflow = 0;
        for (int i = 0; i < lhs.size(); i++) {
            int lhsIdx = lhs.size() - i - 1;
            // must cast to long because java bad
            long shled = ((long) lhs.getByte(lhsIdx)) << shlAmt;
            byte remainder = (byte) (shled & 0xff);
            byte result = (byte) (remainder | overflow);
            overflow = (shled & ~0xff | overflow) >>> 8;
            lhs.set(lhsIdx, result);

        }
        // Write it back to the original register
        this.writeToRegister(regiIdx, false, lhs, mother, rand);
    }

    private void opcJMP(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);
        this.currentIP = BinaryUtils.toLong(destination);
    }

    private void opcJZ(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList switcher = this.readIV(this.currentIP, true, mother, rand);
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);

        long switcherLong = BinaryUtils.toLong(switcher);
        if (switcherLong == 0) {
            this.currentIP = BinaryUtils.toLong(destination);
        }
    }

    private void opcJNZ(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList switcher = this.readIV(this.currentIP, true, mother, rand);
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);

        long switcherLong = BinaryUtils.toLong(switcher);
        if (switcherLong != 0) {
            this.currentIP = BinaryUtils.toLong(destination);
        }
    }

    private void opcJLZ(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList switcher = this.readIV(this.currentIP, true, mother, rand);
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);

        long switcherLong = BinaryUtils.toLong(switcher);
        if (switcherLong < 0) {
            this.currentIP = BinaryUtils.toLong(destination);
        }
    }

    private void opcJGZ(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList switcher = this.readIV(this.currentIP, true, mother, rand);
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);

        long switcherLong = BinaryUtils.toLong(switcher);
        if (switcherLong > 0) {
            this.currentIP = BinaryUtils.toLong(destination);
        }
    }

    private void opcPRINT(MotherboardRepr mother, Random rand) throws Emergency {
        // to print, to print, l'chaim
        ByteList toPrint = this.readIV(this.currentIP, true, mother, rand);
        System.out.println(BinaryUtils.smolHexdump(toPrint));
    }

    private void opcDEBUG(MotherboardRepr mother, Random rand) throws Emergency {
        // For now
        ByteList startIdxList = this.readIV(this.currentIP, true, mother, rand);
        ByteList lengthList = this.readIV(this.currentIP, true, mother, rand);
        int startIdx = (int) BinaryUtils.toLong(startIdxList);
        int length = (int) BinaryUtils.toLong(lengthList);

        System.out.println(BinaryUtils.hexdump(new ByteArrayList(mother.memory, startIdx, length)));
    }

    // endregion

    // region Reading

    /**
     * Read an opcode and increment currentIP.
     */
    private Opcode readOpcode(MotherboardRepr mother, Random rand) throws Emergency {
        byte opcodeByte = read(this.currentIP, 1, Permissions.RX, mother, rand).getByte(0);
        Opcode opcode = OPCODES_TO_BYTECODE.inverse().getOrDefault(opcodeByte, ILLEGAL);
//        Utils.debugf("> %s (0x%2x) @ 0x%x\n", opcode, opcodeByte, this.currentIP);
        this.currentIP += 1;
        return opcode;
    }


    /**
     * Read a register value from bytecode and return the value in it.
     * If the given IP is the same as currentIP, advance the IP.
     */
    private ByteList readRegister(long ip, boolean advanceIP, MotherboardRepr mother, Random rand) throws Emergency {
        byte bytecode = read(ip, 1, Permissions.RX, mother, rand).getByte(0);
        if (advanceIP) {
            this.currentIP++;
        }
        if ((bytecode & 0b10000000) != 0) {
            // this is an indexed register
            int index = bytecode & 0b01111111;
            try {
                int motherIdx = Utils.getUncertainNestedInt(this.registers, index, rand);
                RegisterRepr register = mother.registers.get(motherIdx);
                return new ByteArrayList(register.value);
            } catch (IndexOutOfBoundsException e) {
                throw new Emergency();
            }
        } else {
            // this is a special register
            // we can never return a ByteLists.singleton because those are immutable.
            if (bytecode == 0) {
                // nil
                return new ByteArrayList(new byte[]{(byte) 0});
            } else if (bytecode == 1) {
                // IP
                return new ByteArrayList(this.IP);
            } else if (bytecode == 2) {
                // SP
                return new ByteArrayList(this.SP);
            } else if (bytecode == 3) {
                // FLAGS
                return new ByteArrayList(this.FLAGS);
            } else {
                // o no
                throw new Emergency();
            }
        }
    }

    /**
     * Read an external location from the given IP and return the value in it.
     */
    private ByteList readExternal(long ip, boolean advanceIP, int amount, MotherboardRepr mother,
            Random rand) throws Emergency {
        byte header = read(ip, 1, Permissions.RX, mother, rand).getByte(0);
        if (advanceIP) {
            this.currentIP++;
        }
        if ((header & 0b10000000) == 0b10000000) {
            // Data interface
            throw new NotImplementedException("reading from stackvalues NYI");
        } else {
            // Mem location
            ByteList memLocList = this.readIV(advanceIP ? this.currentIP : ip + 1, advanceIP, mother, rand);
            long memLoc = BinaryUtils.toLong(memLocList);
            return this.read(memLoc, amount, Permissions.R, mother, rand);
        }
    }

    /**
     * Read an IV and return the value in it.
     */
    private ByteList readIV(long ip, boolean advanceIP, MotherboardRepr mother, Random rand) throws Emergency {
        byte header = read(ip, 1, Permissions.R, mother, rand).getByte(0);
        if ((header & 0b11000000) == 0b01000000) {
            return readLiteral(ip, advanceIP, mother, rand);
        } else if ((header & 0b11100000) == 0b00100000) {
            // it's a stackvalue
            throw new NotImplementedException("stackvalues aren't implemented yet");
        } else {
            // register
            return readRegister(ip, advanceIP, mother, rand);
        }
    }

    /**
     * Read a literal value from the given location.
     */
    private ByteList readLiteral(long ip, boolean advanceIP, MotherboardRepr mother, Random rand) throws Emergency {
        byte rawLen = read(ip, 1, Permissions.R, mother, rand).getByte(0);
        int length = rawLen & 0b00111111; // cut out the 0b01 at the start
        ByteList value = read(ip + 1, length, Permissions.R, mother, rand);

        if (advanceIP) {
            this.currentIP += length + 1; // length of the literal, + 1 for the header
        }
        return value;
    }

    /**
     * Read a value from the stack given an offset and length.
     * <p>
     * TODO: prevent nested stackvalues (it's already prevented by the compiler, but Asshole McGee could
     * manually insert bytecode...)
     */
    private ByteList readFromStack(long offset, int length, MotherboardRepr mother, Random rand) throws Emergency {
        long absoluteIdx = this.memoryStarts.get(MemoryType.RAM) + BinaryUtils.toLong(this.SP) + offset;
        return this.read(absoluteIdx, length, Permissions.R, mother, rand);
    }

    // endregion reading

    // region writing

    /**
     * Write a value to the register indicated by the given value.
     * Also increments currentIP (if it's the same as ip) and updates movement flags.
     */
    // TODO: Turn an IndexOutOfBounds into an Emergency
    private void writeToRegister(long ip, boolean advanceIP, ByteList value, MotherboardRepr mother,
            Random rand) throws Emergency {
        byte registerCode = read(ip, 1, Permissions.R, mother, rand).getByte(0);
        if (advanceIP) {
            this.currentIP++;
        }
        if ((registerCode & 0b10000000) != 0) {
            // indexed time
            int ownIdx = registerCode & 0b01111111;
            int motherIdx = Utils.getUncertainNestedInt(this.registers, ownIdx, rand);
            RegisterRepr register = mother.registers.get(motherIdx);

            // TODO: refactor to use .getArray because it uses fast syscalls
            for (int i = 0; i < register.value.length; i++) {
                int valIdx = value.size() - i - 1;
                int regiIdx = register.value.length - i - 1;
                if (valIdx < 0) {
                    // we are trying to read more than this value has
                    // write a 0 as per spec
                    register.value[regiIdx] = 0;
                } else {
                    register.value[regiIdx] = value.getByte(valIdx);
                }
            }

            // Set move flags
            if (value.size() < register.value.length) {
                // expand
                this.FLAGS[0] |= EXPAND_BIT;
            } else {
                // expandn't
                this.FLAGS[0] &= EXPAND_MASK;
            }
            if (register.value.length < value.size()) {
                // squish
                this.FLAGS[0] |= SQUISH_BIT;
            } else {
                // squishn't
                this.FLAGS[0] &= SQUISH_MASK;
            }
        } else {
            // special register
            if (registerCode == 0) {
                // nil; discard the value by doing nothing
            } else if (registerCode == 1) {
                // IP
                this.currentIP = BinaryUtils.toLong(value);
            } else if (registerCode == 2) {
                // SP
                for (int i = 0; i < this.SP.length; i++) {
                    int spIdx = this.SP.length - i - 1;
                    int valIdx = value.size() - i - 1;
                    if (valIdx >= 0) {
                        this.SP[spIdx] = value.getByte(valIdx);
                    } else {
                        this.SP[spIdx] = 0;
                    }
                }
            }
        }
    }

    /**
     * Write to the given offset of the SP.
     */
    private void writeToStackvalue(ByteList value, long offset, MotherboardRepr mother, Random rand) throws Emergency {
        long absoluteIdx = this.memoryStarts.get(MemoryType.RAM) + BinaryUtils.toLong(this.SP) + offset;

        this.write(absoluteIdx, value, Permissions.R, mother, rand);
    }

    /**
     * Write a value to the external location at the given IP.
     */
    private void writeToExternal(long ip, boolean advanceIP, ByteList value, MotherboardRepr mother,
            Random rand) throws Emergency {
        byte header = this.read(ip, 1, Permissions.R, mother, rand).getByte(0);
        if (advanceIP) {
            this.currentIP++;
        }
        if ((header & 0b10000000) != 0) {
            // dataface
            throw new NotImplementedException("writing to datafaces NYI");
        } else {
            // it's a memory location
            ByteList memLocList = this.readIV(advanceIP ? this.currentIP : ip + 1, advanceIP, mother, rand);
            long memLoc = BinaryUtils.toLong(memLocList);
            this.write(memLoc, value, new Permissions(false, true, false), mother, rand);
        }
    }

    // endregion writing

    /**
     * Read a register from bytecode and return (motherboard index, reference to it).
     * * Index < 0 means a special register:
     * * -1: NIL
     * * -2: IP
     * * -3: SP
     * * -4: FLAGS
     * If it's NIL or FLAGS, the reference will be null.
     * <p>
     * Returns null if the register couldn't be found.
     */
    @Nullable
    private Pair<Integer, RegisterRepr> fetchRegisterInfo(long ip, boolean advanceIP, MotherboardRepr mother,
            Random rand) {
        try {
            byte bytecode = read(ip, 1, Permissions.RX, mother, rand).getByte(0);
            if (advanceIP) {
                this.currentIP++;
            }
            if ((bytecode & 0b10000000) != 0) {
                // this is an indexed register
                int index = bytecode & 0b01111111;
                try {
                    int motherIdx = Utils.getUncertainNestedInt(this.registers, index, rand);
                    RegisterRepr register = mother.registers.get(motherIdx);
                    return new Pair<>(
                            motherIdx,
                            register
                    );
                } catch (IndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                // this is a special register
                if (bytecode == 0) {
                    // nil
                    return new Pair<>(-1, null);
                } else if (bytecode == 1) {
                    // IP
                    return new Pair<>(-2, this.ipExtender);
                } else if (bytecode == 2) {
                    // SP
                    return new Pair<>(-3, this.spExtender);
                } else if (bytecode == 3) {
                    // FLAGS
                    return new Pair<>(-4, null);
                } else {
                    // o no
                    return null;
                }
            }
        } catch (Emergency e) {
            return null;
        }

    }

    // endregion
}
