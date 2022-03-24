package me.gammadelta.vcc.common.block;

import net.minecraft.state.BooleanProperty;
import net.minecraft.state.Property;


public class VCCBlockStates {
    /**
     * Property for blocks that tick.
     * (CPUs and motherboards)
     */
    public static final Property<Boolean> TICKING = BooleanProperty.create("ticking");
}
