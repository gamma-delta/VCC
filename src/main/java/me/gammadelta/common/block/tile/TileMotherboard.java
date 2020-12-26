package me.gammadelta.common.block.tile;

import me.gammadelta.VCCMod;
import me.gammadelta.common.program.MotherboardRepr;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class TileMotherboard extends TileEntity implements ITickableTileEntity {
    // We don't need to serialize this because the client doesn't need to know about it.
    private MotherboardRepr motherboard;
    // used for executing a frame on a high power signal
    private boolean wasPoweredLastTick = false;

    // boy i would love to know what this constructor means or does
    // as far as i know it's magic.
    public TileMotherboard(TileEntityType<?> type) {
        super(type);
    }


    @Override
    public void tick() {
        if (world.isRemote) {
            // this means "is this code running on the client?"
            // in that case I don't want to do anything!
            return;
        }

        int numberOfStepsToTake = this.motherboard.overclocks.size();
        // make a rising edge tick the motherboard
        boolean powered = world.isBlockPowered(this.getPos());
        if (!this.wasPoweredLastTick && powered) {
            numberOfStepsToTake++;
        }
        this.wasPoweredLastTick = powered;

        if (numberOfStepsToTake > 0) {
            VCCMod.LOGGER.info("Ticking {} times", numberOfStepsToTake);
        }
    }


}
