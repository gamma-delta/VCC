package me.gammadelta.common.block.tile;

import me.gammadelta.Utils;
import me.gammadelta.VCCMod;
import me.gammadelta.VCCRegistry;
import me.gammadelta.common.program.MotherboardRepr;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.Constants;

import java.util.Arrays;
import java.util.Set;

public class TileMotherboard extends TileEntity implements ITickableTileEntity {
    // We don't need to serialize this because the client doesn't need to know about it.
    private MotherboardRepr motherboard;
    // used for executing a frame on a high power signal
    private boolean wasPoweredLastTick = false;

    public TileMotherboard() {
        super(VCCRegistry.Tiles.MOTHERBOARD.get());
    }

    public void updateConnectedComponents() {
        Set<TileDumbComputerComponent> found = Utils.findDumbComponents(pos, world);
        // for now
        VCCMod.LOGGER.info(Arrays.toString(found.toArray()));
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

        BlockState bs = world.getBlockState(pos);
        if (bs.get(BlockStateProperties.LIT) == (numberOfStepsToTake == 0)) {
            // we want it to be lit if we are taking any steps right now
            // also sorry the boolean statement above is complicated ;-;
            world.setBlockState(pos, bs.with(BlockStateProperties.LIT, numberOfStepsToTake != 0),
                    // dunno what these do but mcjty has them
                    Constants.BlockFlags.NOTIFY_NEIGHBORS + Constants.BlockFlags.BLOCK_UPDATE);
        }
    }
}
