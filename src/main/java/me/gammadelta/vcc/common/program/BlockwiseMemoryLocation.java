package me.gammadelta.vcc.common.program;

/**
 * A memory location based not on a byte index, but on
 * which number and which type it indexes into.
 */
public class BlockwiseMemoryLocation {
    public final long originalByteIdx;
    public final int blockIdx;
    public final MemoryType memType;

    public BlockwiseMemoryLocation(long byteIdx, int blockIdx, MemoryType memType) {
        this.originalByteIdx = byteIdx;
        this.blockIdx = blockIdx;
        this.memType = memType;
    }
}
