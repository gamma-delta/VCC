package me.gammadelta.common.program;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.bytes.ByteLists;
import it.unimi.dsi.fastutil.ints.IntList;
import me.gammadelta.Utils;
import me.gammadelta.common.program.compilation.Opcode;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Random;

import static me.gammadelta.common.program.compilation.Opcode.ILLEGAL;
import static me.gammadelta.common.program.compilation.Opcode.OPCODES_TO_BYTECODE;

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

    public byte FLAGS = 0;
    public byte[] IP;
    public byte[] SP;
    // These references really only exist to prevent them from being dropped.
    // Most things should be done through the IP and SP variables.
    @Nullable
    public RegisterRepr ipExtender;
    @Nullable
    public RegisterRepr spExtender;

    // Used to make working with the IP in the middle of a operation not difficult
    // It's set to the IP at the beginning of every step and written back.
    private long currentIP;

    /**
     * Where the CPU block that pretends to be this is
     */
    public BlockPos manifestation;

    /**
     * Cached indexes of registers this CPU can see.
     * The indexes refer to the array of registers in the motherboard.
     * Closest -> farthest. Equidistant ones are in subarrays.
     */
    public ArrayList<IntList> registers;
    /**
     * Cached total number of registers
     */
    public int registerCount;

    /**
     * Cached indexes of datafaces this CPU can see.
     * The indexes refer to the array of datafaces in the motherboard.
     */
    public ArrayList<IntList> datafaces;
    public int datafaceCount;

    /**
     * Each array stores the indices that this CPU thinks the given memory is at.
     */
    public EnumMap<MemoryType, ArrayList<IntList>> memoryLocations;
    public EnumMap<MemoryType, Integer> memoryCounts;

    public CPURepr(@Nullable RegisterRepr ipExtender, @Nullable RegisterRepr spExtender, BlockPos manifestation,
            ArrayList<IntList> registers, ArrayList<IntList> datafaces,
            EnumMap<MemoryType, ArrayList<IntList>> memoryLocations) {
        this.manifestation = manifestation;
        this.ipExtender = ipExtender;
        this.spExtender = spExtender;

        this.registers = registers;
        this.registerCount = this.registers.stream().mapToInt(IntList::size).sum();
        this.datafaces = datafaces;
        this.datafaceCount = this.datafaces.stream().mapToInt(IntList::size).sum();
        this.memoryLocations = memoryLocations;
        this.memoryCounts = new EnumMap<>(MemoryType.class);
        memoryLocations.forEach(((memType, groups) -> {
            int memCount = groups.stream().mapToInt(IntList::size).sum();
            this.memoryCounts.put(memType, memCount);
        }));

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
    }

    /**
     * Cut down constructor mostly for testing purposes.
     */
    public CPURepr(ArrayList<IntList> registers,
            EnumMap<MemoryType, ArrayList<IntList>> memoryLocations) {
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
    public byte[] read(long readIdx, int readThisMuch, Permissions perms, MotherboardRepr mother,
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
        return gotRead.toArray(new byte[0]);
    }

    // Write bytes to a memory location
    public void write(long writeIdx, ByteArrayList toWrite, Permissions perms, MotherboardRepr mother,
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

    public void executeStep(MotherboardRepr mother, Random rand) {
        this.currentIP = Utils.toLong(this.IP);
        try {
            Opcode opcode = this.readOpcode(mother, rand);

            switch (opcode) {
                case NOP:
                    // boy, that was easy to implement
                    break;

                case MOV:
                    this.opcMOV(mother, rand);
                    break;

                case INC:
                    this.opcINC(mother, rand);
                    break;
                case AND:
                    this.opcAND(mother, rand);
                    break;
                case SUB:
                    this.opcSUB(mother, rand);
                    break;

                case JMP:
                    this.opcJMP(mother, rand);
                    break;
                case JNZ:
                    this.opcJNZ(mother, rand);
                    break;

                case PRINT:
                    this.opcPRINT(mother, rand);
                    break;

                default:
                    throw new NotImplementedException(
                            String.format("%s is not implemented (at idx 0x%x)", opcode, this.currentIP - 1));
            }

            // Put currentIP back into the IP to make our lives easier
            // Discard the squish/stretch, we don't need it
            // TODO: write to move flags when incrementing the IP?
            Utils.writeLong(this.currentIP, this.IP);
        } catch (Emergency e) {
            // oops. go to exram, go directly to exram
            if (this.memoryCounts.get(MemoryType.EXRAM) == 0) {
                // actually go to the start
                Utils.writeLong(0, this.IP);
            } else {
                // go to (a randomly selected) exram
                int motherExramIdx = Utils.getUncertainNestedInt(this.memoryLocations.get(MemoryType.EXRAM), 0, rand);
                try {
                    int exramByteIdx = mother.getRegionIndex(MemoryType.EXRAM, motherExramIdx);
                    Utils.writeLong(exramByteIdx, this.IP);
                } catch (Emergency e2) {
                    // just set it to zero, whatever
                    Utils.writeLong(0, this.IP);
                }
            }
        }
    }


    // region Opcode implementation

    private void opcMOV(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList source = this.readIV(this.currentIP, true, mother, rand);
        this.writeToRegister(this.currentIP, true, source, mother, rand);
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

    private void opcSUB(MotherboardRepr mother, Random rand) throws Emergency {
        long srcIdx = this.currentIP;
        ByteList lhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList rhs = this.readRegister(this.currentIP, true, mother, rand);
        ByteList underflow = Utils.subMut(lhs, rhs);
        this.writeToRegister(srcIdx, false, lhs, mother, rand);

        // Underflow?
        // TODO: underflow doesn't work properly
        if (underflow.size() > 0) {
            this.FLAGS |= UNDERFLOW_BIT;
        } else {
            this.FLAGS &= UNDERFLOW_MASK;
        }
        // no overflow for you
        this.FLAGS &= OVERFLOW_MASK;
    }

    private void opcINC(MotherboardRepr mother, Random rand) throws Emergency {
        long regiIdx = this.currentIP;
        ByteList val = this.readRegister(this.currentIP, true, mother, rand);
        ByteList remainder = Utils.addMut(val, ByteLists.singleton((byte) 1));
        this.writeToRegister(regiIdx, false, val, mother, rand);
        // Overflow flag?
        if (remainder.size() > 0) {
            // oof
            this.FLAGS |= OVERFLOW_BIT;
        } else {
            this.FLAGS &= OVERFLOW_MASK;
        }
        // always clear the underflow bit
        this.FLAGS &= UNDERFLOW_MASK;
    }

    private void opcJNZ(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList switcher = this.readIV(this.currentIP, true, mother, rand);
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);

        long switcherLong = Utils.toLong(switcher);
        if (switcherLong != 0) {
            this.currentIP = Utils.toLong(destination);
        }
    }

    private void opcJMP(MotherboardRepr mother, Random rand) throws Emergency {
        ByteList destination = this.readIV(this.currentIP, true, mother, rand);
        this.currentIP = Utils.toLong(destination);
    }


    private void opcPRINT(MotherboardRepr mother, Random rand) throws Emergency {
        // to print, to print, l'chaim
        ByteList toPrint = this.readIV(this.currentIP, true, mother, rand);
        System.out.println(Utils.smolHexdump(toPrint));
    }

    // endregion

    // region Reading

    /**
     * Read an opcode and increment currentIP.
     */
    private Opcode readOpcode(MotherboardRepr mother, Random rand) throws Emergency {
        byte opcodeByte = read(this.currentIP, 1, Permissions.RX, mother, rand)[0];
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
        byte bytecode = read(ip, 1, Permissions.RX, mother, rand)[0];
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
            if (bytecode == 0) {
                // nil
                return ByteLists.singleton(0);
            } else if (bytecode == 1) {
                // IP
                return new ByteArrayList(this.IP);
            } else if (bytecode == 2) {
                // SP
                return new ByteArrayList(this.SP);
            } else if (bytecode == 3) {
                // FLAGS
                return ByteLists.singleton(this.FLAGS);
            } else {
                // o no
                throw new Emergency();
            }
        }
    }

    /**
     * Read an IV. If the given ip is the same as currentIP, advance currentIP.
     */
    private ByteList readIV(long ip, boolean advanceIP, MotherboardRepr mother, Random rand) throws Emergency {
        byte header = read(ip, 1, Permissions.R, mother, rand)[0];
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
        byte rawLen = read(ip, 1, Permissions.R, mother, rand)[0];
        int length = rawLen & 0b00111111; // cut out the 0b01 at the start
        byte[] value = read(ip + 1, length, Permissions.R, mother, rand);

        if (advanceIP) {
            this.currentIP += length + 1; // length of the literal, + 1 for the header
        }
        return new ByteArrayList(value);
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
        byte registerCode = read(ip, 1, Permissions.R, mother, rand)[0];
        if (advanceIP) {
            this.currentIP++;
        }
        if ((registerCode & 0b10000000) != 0) {
            // indexed time
            int ownIdx = registerCode & 0b01111111;
            int motherIdx = Utils.getUncertainNestedInt(this.registers, ownIdx, rand);
            RegisterRepr register = mother.registers.get(motherIdx);

            // TODO: refactor to use .getArray because it uses fast syscalls
            for (int i = 0; i < value.size(); i++) {
                int valIdx = value.size() - i - 1;
                int regiIdx = register.value.length - i - 1;
                if (regiIdx >= register.value.length) {
                    break;
                }
                if (valIdx >= value.size()) {
                    // write a 0 as per spec
                    register.value[regiIdx] = 0;
                } else {
                    register.value[regiIdx] = value.getByte(valIdx);
                }
            }

            // Set move flags
            if (value.size() < register.value.length) {
                // expand
                this.FLAGS |= EXPAND_BIT;
            } else {
                // expandn't
                this.FLAGS &= EXPAND_MASK;
            }
            if (register.value.length < value.size()) {
                // squish
                this.FLAGS |= SQUISH_BIT;
            } else {
                // squishn't
                this.FLAGS &= SQUISH_MASK;
            }
        } else {
            throw new NotImplementedException("writing to special registers NYI");
        }
    }

    // endregion writing


    // endregion
}
