package me.gammadelta.common.block.tile;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

/**
 * Any part of a computer that is not the motherboard.
 * (or a drive).
 * <p>
 * This just stores the location of the motherboard.
 */
public abstract class TileDumbComputerComponent extends TileEntity {
    // This will be null if it can't find its motherboard
    @Nullable
    public BlockPos motherboardLocation;
    private static final String MOTHERBOARD_LOC_KEY = "motherboard_location";
    // Used to track if you break and replace the motherboard
    @Nullable
    public UUID motherboardUUID;
    private static final String KEY_MOTHERBOARD_UUID = "motherboard_uuid";

    public TileDumbComputerComponent(TileEntityType<?> iWishIKnewWhatThisConstructorDidSadFace) {
        super(iWishIKnewWhatThisConstructorDidSadFace);
    }

    @Override
    public void remove() {
        TileMotherboard motherboard = this.getMotherboard(world);
        if (motherboard != null && motherboard.getUUID() == this.motherboardUUID) {
            motherboard.updateConnectedComponents();
        }
        super.remove();
    }

    /**
     * Called by the motherboard when searching for components.
     */
    @SuppressWarnings("unused")
    public void setMotherboard(@Nullable TileMotherboard motherboard) {
        if (motherboard != null) {
            this.motherboardLocation = motherboard.getPos();
            this.motherboardUUID = motherboard.getUUID();
        } else {
            this.motherboardLocation = null;
            this.motherboardUUID = null;
        }
    }

    @Nullable
    public TileMotherboard getMotherboard(IWorld world) {
        if (this.motherboardLocation == null) {
            return null;
        }
        TileEntity gotten = world.getTileEntity(this.motherboardLocation);
        if (gotten == null) {
            return null;
        }
        if (gotten instanceof TileMotherboard) {
            TileMotherboard mother = (TileMotherboard) gotten;
            if (mother.getUUID() != this.motherboardUUID) {
                // this means someone broke and replaced the motherboard, or something
                return null;
            }
            return mother;
        } else {
            return null;
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);

        CompoundNBT maybeMotherboardLoc = tag.getCompound(MOTHERBOARD_LOC_KEY);
        if (!maybeMotherboardLoc.isEmpty()) {
            this.motherboardLocation = NBTUtil.readBlockPos(maybeMotherboardLoc);
        } else {
            this.motherboardLocation = null;
        }
        if (tag.hasUniqueId(KEY_MOTHERBOARD_UUID)) {
            this.motherboardUUID = tag.getUniqueId(KEY_MOTHERBOARD_UUID);
        } else {
            this.motherboardUUID = null;
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public CompoundNBT write(CompoundNBT tag) {
        if (this.motherboardLocation != null) {
            tag.put(MOTHERBOARD_LOC_KEY, NBTUtil.writeBlockPos(this.motherboardLocation));
        }
        if (this.motherboardUUID != null) {
            tag.putUniqueId(KEY_MOTHERBOARD_UUID, this.motherboardUUID);
        }
        return super.write(tag);
    }
}
