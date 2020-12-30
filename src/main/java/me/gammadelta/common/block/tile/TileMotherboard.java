package me.gammadelta.common.block.tile;

import me.gammadelta.VCCMod;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.utils.FloodUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TileMotherboard extends TileEntity implements ITickableTileEntity {
    // region Serialized values

    private MotherboardRepr motherboard;
    private static String MOTHERBOARD_KEY = "motherboard";

    // used for executing a frame on a high power signal
    private boolean wasPoweredLastTick = false;
    private static String POWERED_KEY = "was_powered_last_tick";
    private int ticksSinceLastStepped = TICK_LIT_TIME + 1;
    private static String TICKS_SINCE_LAST_STEPPED_KEY = "ticks_since_last_stepped";

    // endregion

    // how many ticks ago we need to be stepped in order to display lit
    private static final int TICK_LIT_TIME = 10;

    public TileMotherboard() {
        super(VCCBlocks.MOTHERBOARD_TILE.get());
        this.motherboard = new MotherboardRepr();
    }

    public UUID getUUID() {
        return this.motherboard.uuid;
    }

    public List<BlockPos> getControlledBlocks() {
        return this.motherboard.ownedBlocks;
    }

    public void updateConnectedComponents() {
        Set<TileDumbComputerComponent> found = FloodUtils.findUnclaimedComponents(this);
        this.motherboard.updateComponents(this, found.iterator());
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

        boolean needsToBeLit = false;
        if (numberOfStepsToTake > 0) {
            VCCMod.LOGGER.info("Ticking {} times", numberOfStepsToTake);
            ticksSinceLastStepped = 0;
        } else if (ticksSinceLastStepped <= TICK_LIT_TIME) {
            ticksSinceLastStepped++;
            needsToBeLit = true;
        }

        BlockState bs = world.getBlockState(pos);
        boolean isLit = bs.get(BlockStateProperties.LIT);
        if (isLit != needsToBeLit) {
            world.setBlockState(pos, bs.with(BlockStateProperties.LIT, needsToBeLit),
                    // dunno what these do but mcjty has them
                    Constants.BlockFlags.NOTIFY_NEIGHBORS + Constants.BlockFlags.BLOCK_UPDATE);
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        this.wasPoweredLastTick = nbt.getBoolean(POWERED_KEY);
        this.motherboard = new MotherboardRepr(nbt.getCompound(MOTHERBOARD_KEY));
        this.ticksSinceLastStepped = nbt.getInt(TICKS_SINCE_LAST_STEPPED_KEY);

        super.read(state, nbt);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag.putBoolean(POWERED_KEY, this.wasPoweredLastTick);
        tag.put(MOTHERBOARD_KEY, this.motherboard.serialize());
        tag.putInt(TICKS_SINCE_LAST_STEPPED_KEY, this.ticksSinceLastStepped);

        return super.write(tag);
    }

    // kamefrede promises me i need these?

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getPos(), -1, getUpdateTag());
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }
}
