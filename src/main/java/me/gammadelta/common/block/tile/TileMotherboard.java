package me.gammadelta.common.block.tile;

import me.gammadelta.Utils;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.program.MotherboardRepr;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.util.Constants;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TileMotherboard extends TileEntity implements ITickableTileEntity {
    // region Serialized values

    private MotherboardRepr motherboard;
    private static String MOTHERBOARD_TAG = "motherboard";

    // used for executing a frame on a high power signal
    private boolean wasPoweredLastTick = false;
    private static String POWERED_TAG = "was_powered_last_tick";

    // endregion

    public TileMotherboard() {
        super(VCCBlocks.MOTHERBOARD_TILE.get());
    }

    public UUID getUUID() {
        return this.motherboard.uuid;
    }

    public List<BlockPos> getControlledBlocks() {
        return this.motherboard.ownedBlocks;
    }

    public void updateConnectedComponents() {
        Set<TileDumbComputerComponent> found = Utils.findDumbComponents(pos, world);
        // for now
        System.out.println(Arrays.toString(found.toArray()));
        this.motherboard = new MotherboardRepr(pos, found.iterator());
    }

    @Override
    public void tick() {
        if (world.isRemote) {
            // this means "is this code running on the client?"
            // in that case I don't want to do anything!
            return;
        }
        try {
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
            boolean isLit = bs.get(BlockStateProperties.LIT);
            boolean needsToBeLit = numberOfStepsToTake > 0;
            if (isLit != needsToBeLit) {
                // we want it to be lit if we are taking any steps right now
                // also sorry the boolean statement above is complicated ;-;
                world.setBlockState(pos, bs.with(BlockStateProperties.LIT, needsToBeLit),
                        // dunno what these do but mcjty has them
                        Constants.BlockFlags.NOTIFY_NEIGHBORS + Constants.BlockFlags.BLOCK_UPDATE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            world.getServer().getPlayerList().func_232641_a_(new StringTextComponent(
                    String.format("Uh-oh, something bad happened to the motherboard at (%s). Check the server logs.",
                            pos.getCoordinatesAsString())), ChatType.CHAT, Util.DUMMY_UUID);
            world.destroyBlock(pos, true);
            world.removeTileEntity(pos);
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        this.wasPoweredLastTick = nbt.getBoolean(POWERED_TAG);
        this.motherboard = new MotherboardRepr(nbt.getCompound(MOTHERBOARD_TAG));

        super.read(state, nbt);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag.putBoolean(POWERED_TAG, this.wasPoweredLastTick);
        tag.put(MOTHERBOARD_TAG, this.motherboard.serialize());

        return super.write(tag);
    }
}
