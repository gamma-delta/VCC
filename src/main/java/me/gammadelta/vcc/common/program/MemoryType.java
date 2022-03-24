package me.gammadelta.vcc.common.program;

import net.minecraft.util.IStringSerializable;

/**
 * The types of memory.
 * These are declared in the same order they appear to a CPU,
 * so .values can be used to check things.
 */
public enum MemoryType implements IStringSerializable {
    XRAM(256, Permissions.RWX),
    EXRAM(256, Permissions.RWX),
    ROM(1048576, Permissions.R),
    RAM(16384, Permissions.RW);

    public final int storageAmount;
    public final Permissions perms;

    MemoryType(int storageAmount, Permissions perms) {
        this.storageAmount = storageAmount;
        this.perms = perms;
    }

    @Override
    public String getString() {
        return this.name();
    }
}
