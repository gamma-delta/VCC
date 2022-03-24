package me.gammadelta.vcc.api;

import it.unimi.dsi.fastutil.bytes.ByteList;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Capability for things that interact with data.
 * In the vanilla mod, this is used by datafaces.
 */
public interface IDataIOer {
    /**
     * When something tries to push data to me, what happens?
     */
    enum PushResult {
        /** I accepted the data; you can go on with your business. */
        ACCEPT,
        /**
         * You have to block.
         * Either I'm busy handling something, or I can't actually accept data.
         * This is the default.
         */
        BLOCK,
    }

    /**
     * Call this when you want to push data to this block.
     *
     * Returns whether the data was accepted. If not, try again next tick.
     *
     * If simulate is TRUE, don't actually update anything, but still see if you
     * *could* push something.
     */
    PushResult pushDataTo(ByteList data, boolean simulate);

    /**
     * Try to read data from this.
     *
     * If this returns a non-null value, that's the data read.
     * If it returns null, it doesn't have any data to send;
     * either it's busy, or doesn't output any data.
     * In that case, you should block and try again next tick.
     *
     * Note the difference between a 0-length list and null:
     * The first means "Here's some data; there's just none of it."
     * The second means "Hold on a second, I don't even know if I have anything for you."
     *
     * If simulate is TRUE, don't actually update anything, but still return what would be returned.
     */
    @Nullable
    ByteList readDataFrom(boolean simulate);

    /**
     * Here's a reference implementation.
     * This has one "slot" for data.
     * Pushing data to it when it's empty fills it; pushing when it's full blocks.
     * Reading data clears itself and returns the data, or blocks if there is no data.
     */
    class ReferenceImpl implements IDataIOer {
        protected ByteList memory = null;

        public ReferenceImpl() {}

        @Override
        public PushResult pushDataTo(ByteList data, boolean simulate) {
            if (this.memory != null) {
                // No pushing memory if this is full!
                return PushResult.BLOCK;
            }
            if (!simulate) {
                // Only actually set the data if it's not null
                this.memory = data;
            }
            return PushResult.ACCEPT;
        }

        @Nullable
        @Override
        public ByteList readDataFrom(boolean simulate) {
            // Conveniently, out will be null if this.memory is null.
            // And if there's no memory, it wants us to return null!
            ByteList out = this.memory;
            if (!simulate) {
                this.memory = null;
            }
            return out;
        }
    }
}
