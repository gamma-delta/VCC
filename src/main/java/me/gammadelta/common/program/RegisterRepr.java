package me.gammadelta.common.program;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

/**
 * I am the very model of a modern major register
 * My blocks each hold an 8-bit word and side-by-side work together
 * Th'importance of my job to do is really something radical
 * For iteration, processing, and all things mathematical...
 * <p>
 * I'm very good at 'rithmetic and working with an interface
 * But if your CPUs are fast, there just might be a data race
 * But still, for holding data, math, or anything numerical
 * I am the very model of a modern major register!
 */
public class RegisterRepr {
    /**
     * Locations of the original blocks
     */
    public BlockPos[] manifestations;
    private static final String MANIFESTATIONS_TAG = "manifestations";

    /**
     * Value stored in the cluster.
     */
    public byte[] value;
    private static final String VALUE_TAG = "value";

    public RegisterRepr(BlockPos[] manifestations) {
        this.manifestations = manifestations;
        // initialize value zeroed
        // TODO: if i end up making memory scramble on startup, that has to happen *in the startup*.
        this.value = new byte[this.manifestations.length];
    }

    /**
     * Deserialize a RegisterRepr from NBT.
     */
    public RegisterRepr(CompoundNBT tag) {
        ListNBT maniTag = tag.getList(MANIFESTATIONS_TAG, Constants.NBT.TAG_COMPOUND);
        this.manifestations = new BlockPos[maniTag.size()];
        for (int c = 0; c < maniTag.size(); c++) {
            this.manifestations[c] = NBTUtil.readBlockPos(maniTag.getCompound(c));
        }

        this.value = tag.getByteArray(VALUE_TAG);
    }

    /**
     * Serialize a RegisterRepr to NBT.
     */
    public CompoundNBT serialize() {
        CompoundNBT out = new CompoundNBT();

        ListNBT manis = new ListNBT();
        for (BlockPos m : this.manifestations) {
            manis.add(NBTUtil.writeBlockPos(m));
        }
        out.put(MANIFESTATIONS_TAG, manis);

        out.putByteArray(VALUE_TAG, this.value);

        return out;
    }

    public int getByteCount() {
        // each block = one byte
        return this.manifestations.length;
    }
}
