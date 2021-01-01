package me.gammadelta.common.block.tile;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.utils.FloodUtils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
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
    private static final int TICK_LIT_TIME = 5;

    public TileMotherboard() {
        super(VCCBlocks.MOTHERBOARD_TILE.get());
        this.motherboard = new MotherboardRepr();
    }

    public UUID getUUID() {
        return this.motherboard.uuid;
    }

    public Set<BlockPos> getControlledBlocks() {
        return this.motherboard.ownedBlocks;
    }

    public MotherboardRepr getMotherboard() {
        return motherboard;
    }

    public void updateConnectedComponents() {
        Object2IntOpenHashMap<BlockPos> found = FloodUtils.findUnclaimedComponents(this);
        if (found != null) {
            this.motherboard.updateComponents(this, found);
        }
        this.markDirty();
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

        boolean displayLit = false;
        if (numberOfStepsToTake > 0) {
            ticksSinceLastStepped = 0;
            displayLit = true;
        } else if (ticksSinceLastStepped <= TICK_LIT_TIME) {
            // we're still in the cooldown for showing the litness
            ticksSinceLastStepped++;
            displayLit = true;
        }

        BlockState bs = world.getBlockState(pos);
        boolean isLit = bs.get(BlockStateProperties.LIT);
        if (isLit != displayLit) {
            world.setBlockState(pos, bs.with(BlockStateProperties.LIT, displayLit),
                    // dunno what these do but mcjty has them
                    Constants.BlockFlags.NOTIFY_NEIGHBORS + Constants.BlockFlags.BLOCK_UPDATE);
            this.markDirty();
        }

        if (numberOfStepsToTake > 0) {
            // TODO: Execution!
            this.markDirty();
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        this.wasPoweredLastTick = nbt.getBoolean(POWERED_KEY);
        this.motherboard = new MotherboardRepr(nbt.getCompound(MOTHERBOARD_KEY), this);
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

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        read(world.getBlockState(pkt.getPos()), pkt.getNbtCompound());
    }

    @Nonnull
    @Override
    public CompoundNBT getUpdateTag() {
        return write(new CompoundNBT());
    }
}
