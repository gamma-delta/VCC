package me.gammadelta.common.block.tile;

import me.gammadelta.Utils;
import me.gammadelta.common.program.*;
import me.gammadelta.common.program.compilation.Opcode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;

import java.util.ArrayList;

public class TileMotherboard extends TileEntity {

    public static int XRAM_SIZE = 256;
    public static int ROM_SIZE = 1_048_576;
    public static int RAM_SIZE = 65_536;
    public static Permissions XRAM_PERMS = Permissions.RWX;
    public static Permissions ROM_PERMS = Permissions.R;
    public static Permissions RAM_PERMS = Permissions.RW;

    // region These values are serialized back and forth with NBT.

    /**
     * The contents of all memory blocks.
     * See Memory Layout on the wiki.
     */
    private byte[] memory;


    private int xramCount;
    private static final String XRAM_COUNT_KEY = "xram_count";
    private int exramCount; // why did i name these so poorly
    private static final String EXRAM_COUNT_KEY = "exram_count";
    private int romCount;
    private static final String ROM_COUNT_KEY = "rom_count";
    private int ramCount;
    private static final String RAM_COUNT_KEY = "ram_count";

    /**
     * CPUs, sorted by distance to the motherboard. Ties are in their own sub-arrays.
     *
     * `[ [0], [1, 1, 1], [2, 2], [3], [4] ]`
     */
    private ArrayList<ArrayList<CPURepr>> cpus;
    private static final String CPUS_KEY = "cpus";

    /**
     * Registers, in no particular order.
     * This does not include IP/SP extenders; their reprs are stored in the CPU.
     */
    private ArrayList<RegisterRepr> registers;
    private static final String REGISTERS_KEY = "registers";

    // endregion End serialized values.

    // boy i would love to know what this constructor means or does
    // as far as i know it's magic.
    public TileMotherboard(TileEntityType<?> type) {
        super(type);
    }

    /**
     * Execute one step of the computer!
     */
    private void executeStep(World world) {
        for (ArrayList<CPURepr> cpuGroup : this.cpus) {
            int[] indices = Utils.randomIndices(cpuGroup.size(), world.getRandom());
            for (int cpuIdx : indices) {
                cpuGroup.get(cpuIdx).executeStep(this, world);
            }
        }
    }

    // region Interacting with memory

    /**
     * Read the specified number of bytes from memory, with the given permissions.
     */
    public byte[] readBytes(int startIndex, int count, Permissions perms) throws Emergency {
        byte[] out = new byte[count];
        for (int delta = 0; delta < count; delta++) {
            int idx = startIndex + delta;
            // Check permissions
            Permissions permsHere = getPermissions(idx);
            if (!permsHere.satisfiedBy(perms)) {
                // invalid access!
                throw new Emergency();
            }
            out[delta] = this.memory[idx];
        }

        return out;
    }

    /**
     * Read one byte from memory, with the given permissions.
     */
    public byte readByte(int index, Permissions perms) throws Emergency {
        Permissions permsHere = getPermissions(index);
        if (!permsHere.satisfiedBy(perms)) {
            // invalid access!
            throw new Emergency();
        }
        return this.memory[index];
    }

    /**
     * Write the specified number of bytes to memory, with the given permissions.
     * Note you can pass in Permissions.NONE here to get unfettered write access anywhere.
     */
    public void writeBytes(int startIndex, byte[] newBytes, Permissions perms) throws Emergency {
        for (int delta = 0; delta < newBytes.length; delta++) {
            int idx = startIndex + delta;
            // Check permissions
            Permissions permsHere = getPermissions(idx);
            if (!permsHere.satisfiedBy(perms)) {
                // invalid access!
                throw new Emergency();
            }
            this.memory[idx] = newBytes[delta];
        }
    }

    /**
     * Result of reading from memory to a value.
     * Contains the read value and how many bytes to advance.
     * If the read can fail, the function returning this is responsible for having the emergency.
     */
    public static class Read<T> {
        T value;
        int advanceBy;

        public Read(T value, int advanceBy) {
            this.value = value;
            this.advanceBy = advanceBy;
        }
    }

    /**
     * Read an opcode.
     */
    public Read<Opcode> readOpcode(int index, Permissions perms) throws Emergency {
        byte opcByte = this.readByte(index, perms);
        Opcode opcode = Opcode.OPCODES_TO_BYTECODE.inverse().getOrDefault((short) opcByte, Opcode.ILLEGAL);
        return new Read<>(opcode, 1);
    }

    // region Arguments

    // Why does any language not ship with algebraic data types?
    // I'm going to get them sooner or later. You can't stop me.
    // You are only making your life harder and slower, Java.

    /** Register argument */
    public abstract static class Register {
        /** R0, R7, R34, etc */
        static class Indexed extends Register {
            int index;

            public Indexed(int index) {
                this.index = index;
            }
        }
        /** NIL, IP, SP, or FLAGS */
        static class Special extends Register {
            enum Which {
                NIL, IP, SP, FLAGS
            }
            Which which;

            public Special(Which which) {
                this.which = which;
            }
        }
    }

    /** Immediate argument */
    public abstract static class Immediate {
        /** Literal value */
        static class Literal extends Immediate {
            byte[] value;

            public Literal(byte[] value) {
                this.value = value;
            }
        }

        /** A register acting as an IV */
        static class Register extends Immediate {
            TileMotherboard.Register register;

            public Register(TileMotherboard.Register register) {
                this.register = register;
            }
        }

        /** Stackvalue! Nested StackValues are forbidden. */
        static class StackValue extends Immediate {
            byte offset;
            Immediate backing;

            public StackValue(byte offset, Immediate backing) {
                this.offset = offset;
                this.backing = backing;
            }
        }
    }

    /** External Location argument */
    public abstract static class External {
        /** A dataface */
        static class Dataface extends External {
            int index;

            public Dataface(int index) {
                this.index = index;
            }
        }

        /** A location in memory. */
        static class MemLocation extends External {
            Immediate backing;

            public MemLocation(Immediate backing) {
                this.backing = backing;
            }
        }
    }

    // endregion

    /** Read a register. */
    public Read<Register> readRegisterArg(int index, Permissions perms) throws Emergency {
        // the next regi pokemon: regibyte, introduced with the Cyber type
        byte regiByte = this.readByte(index, perms);
        if ((regiByte & 0b1000_0000) == 0) {
            // Special register
            Register.Special.Which which;
            switch (regiByte) {
                case 0x0: which = Register.Special.Which.NIL; break;
                case 0x1: which = Register.Special.Which.IP; break;
                case 0x2: which = Register.Special.Which.SP; break;
                case 0x3: which = Register.Special.Which.FLAGS; break;
                default: throw new Emergency();
            }
            return new Read<>(new Register.Special(which), 1);
        } else {
            // Boring ol' indexed register
            // must do more bullshit because java doesn't like unsigned byte literals
            byte regiIdx = (byte) ((short) regiByte & 0b01111_1111);
            return new Read<>(new Register.Indexed(regiIdx), 1);
        }
    }

    /** Read an IV. */
    public Read<Immediate> readIVArg(int index, Permissions perms) throws Emergency {
        return this.ivInner(index, perms, false);
    }

    private Read<Immediate> ivInner(int index, Permissions perms, boolean haveWeAlreadyReadAStackvalue) throws Emergency {
        byte theByte = this.readByte(index, perms);
        if ((theByte & 0b1100_0000) == 0b0100_0000) {
            // It's a literal.
            int length = theByte & 0b0011_1111;
            byte[] literalBytes = this.readBytes(index, length, perms);
            // advance the length of the literal, + 1 for the header
            return new Read<>(new Immediate.Literal(literalBytes), length + 1);
        } else if ((theByte & 0b1110_0000) == 0b0010_0000) {
            // It's a stackvalue
            if (haveWeAlreadyReadAStackvalue) {
                // no doing that
                throw new Emergency();
            }
            // Get the offset
            byte offset = (byte) (theByte & 0b0000_1111);
            // Read one more thing
            Read<Immediate> backing = this.ivInner(index + 1, perms, true);
            // and return it all
            // advance 1 for this header, plus however many other we must read
            return new Read<>(new Immediate.StackValue(offset, backing.value), backing.advanceBy + 1);
        } else {
            // It's a register
            Read<Register> readRegi = this.readRegisterArg(index, perms);
            // i could put a literal 1 instead of advanceBy but this is more correct
            return new Read<Immediate>(new Immediate.Register(readRegi.value), readRegi.advanceBy);
        }
    }

    /** Read an external value. */
    public Read<External> readExternalArg(int index, Permissions perms) throws Emergency {
        byte theByte = this.readByte(index, perms);
        if ((theByte & 0b1000_0000) == 0b1000_0000) {
            // Data interface
            int faceIndex = theByte & 0b0111_1111;
            return new Read<>(new External.Dataface(faceIndex), 1);
        } else {
            // it's a memory address; read an IV.
            Read<Immediate> immediate = this.readIVArg(index + 1, perms);
            return new Read<>(new External.MemLocation(immediate.value), immediate.advanceBy + 1);
        }
    }

    /**
     * Get the permissions of the given index.
     * If it's out of bounds, return Permissions.NONE.
     */
    public Permissions getPermissions(int idx) {
        int xramIdx = (this.xramCount + this.exramCount) * XRAM_SIZE;
        int romIdx = xramIdx + this.romCount * ROM_SIZE;
        int ramIdx = xramIdx + this.ramCount * RAM_SIZE;
        if (idx < 0) {
            return Permissions.NONE;
        } else if (idx < xramIdx) {
            return XRAM_PERMS;
        } else if (idx < romIdx) {
            return ROM_PERMS;
        } else if (idx < ramIdx) {
            return RAM_PERMS;
        } else {
            // out of bounds
            return Permissions.NONE;
        }
    }

    // endregion
}
