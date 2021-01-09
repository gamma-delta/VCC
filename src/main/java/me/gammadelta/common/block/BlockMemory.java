package me.gammadelta.common.block;

import me.gammadelta.common.program.MemoryType;

public class BlockMemory extends BlockComponent {
    public static final String XRAM_NAME = "xram";
    public static final String EXRAM_NAME = "exram";
    public static final String RAM_NAME = "ram";
    public static final String ROM_NAME = "rom";

    public final MemoryType type;
    public BlockMemory(MemoryType type) {
        super();
        this.type = type;
    }
}
