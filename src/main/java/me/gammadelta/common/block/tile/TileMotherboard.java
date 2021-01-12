package me.gammadelta.common.block.tile;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.gammadelta.common.block.VCCBlockStates;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.program.CPURepr;
import me.gammadelta.common.program.MotherboardRepr;
import me.gammadelta.common.utils.FloodUtils;
import me.gammadelta.common.utils.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class TileMotherboard extends TileEntity implements ITickableTileEntity {
    // region Serialized values

    private MotherboardRepr motherboard;
    private static String MOTHERBOARD_KEY = "motherboard";

    // used for executing a frame on a high power signal
    private boolean wasPoweredLastTick = false;
    private static String POWERED_KEY = "was_powered_last_tick";

    // endregion

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
        int x = this.pos.getX();
        int y = this.pos.getY();
        int z = this.pos.getZ();
        this.world.playSound(x, y, z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.0f, false);
        for (int i = 0; i < 30; i++) {
            this.world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.05, 0.1, 0.05);
        }

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

        boolean displayTicking = false;

        // TODO: power
        boolean hasSufficientPower = true;
        if (hasSufficientPower) {
            int numberOfStepsToTake = 0;
            for (BlockPos oclockPos : this.motherboard.overclocks) {
                if (!world.isBlockPowered(oclockPos)) {
                    numberOfStepsToTake++;
                }
            }

            // make a rising edge also tick the motherboard
            boolean poweredNow = world.isBlockPowered(this.getPos());
            if (!this.wasPoweredLastTick && poweredNow) {
                // rising edge detected
                numberOfStepsToTake++;
            }
            this.wasPoweredLastTick = poweredNow;

            for (ArrayList<CPURepr> cpuGroup : this.motherboard.cpus) {
                for (int cpuIdx : Utils.randomIndices(cpuGroup.size(), this.world.rand)) {
                    CPURepr cpu = cpuGroup.get(cpuIdx);
                    BlockState cpuState = world.getBlockState(cpu.manifestation);
                    for (int i = 0; i < numberOfStepsToTake; i++) {
                        cpu.executeStep(this.motherboard, world.rand);
                    }
                    boolean execute = numberOfStepsToTake > 0;
                    world.setBlockState(cpu.manifestation, cpuState.with(VCCBlockStates.TICKING, execute));

                    this.markDirty();
                }
            }


            displayTicking = numberOfStepsToTake > 0;
        }

        // Update blockstates
        BlockState bs = world.getBlockState(pos);
        boolean isLit = bs.get(BlockStateProperties.LIT);
        boolean isTicking = bs.get(VCCBlockStates.TICKING);
        if (isLit != hasSufficientPower || isTicking != displayTicking) {
            world.setBlockState(
                    pos,
                    bs
                            .with(BlockStateProperties.LIT, hasSufficientPower)
                            .with(VCCBlockStates.TICKING, displayTicking),
                    Constants.BlockFlags.NOTIFY_NEIGHBORS + Constants.BlockFlags.BLOCK_UPDATE
            );
            this.markDirty();
        }

    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        this.wasPoweredLastTick = nbt.getBoolean(POWERED_KEY);
        this.motherboard = new MotherboardRepr(nbt.getCompound(MOTHERBOARD_KEY), this);

        super.read(state, nbt);
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag.putBoolean(POWERED_KEY, this.wasPoweredLastTick);
        tag.put(MOTHERBOARD_KEY, this.motherboard.serialize());

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
