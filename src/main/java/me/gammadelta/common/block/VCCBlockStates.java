package me.gammadelta.common.block;

import me.gammadelta.common.program.MemoryType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.Property;


public class VCCBlockStates {
    /**
     * Property for blocks that tick.
     * (CPUs and motherboards)
     */
    public static final Property<Boolean> TICKING = BooleanProperty.create("ticking");
}
