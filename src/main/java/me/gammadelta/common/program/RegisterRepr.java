package me.gammadelta.common.program;

import net.minecraft.util.math.BlockPos;

/**
 * One virtual register, consisting of a bunch of RegisterBlocks.
 */
public class RegisterRepr {
    /** Locations of the original blocks */
    public BlockPos[] manifestations;

    /** Value stored in the cluster. */
    public byte[] value;

    public RegisterRepr(BlockPos[] manifestations) {
        this.manifestations = manifestations;
        // initialize value zeroed
        // TODO: if i end up making memory scramble on startup, that has to happen *in the startup*.
        this.value = new byte[this.manifestations.length];
    }

    public int getByteCount() {
        // each block = one byte
        return this.manifestations.length;
    }
}
