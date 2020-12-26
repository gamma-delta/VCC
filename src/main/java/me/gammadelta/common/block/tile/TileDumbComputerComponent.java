package me.gammadelta.common.block.tile;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Any part of a computer that is not the motherboard.
 * (or a drive).
 *
 * This just stores the location of the motherboard.
 */
public abstract class TileDumbComputerComponent extends TileEntity {
    // This will be null if it can't find its motherboard
    @Nullable
    private BlockPos motherboardLocation;
    private static final String MOTHERBOARD_LOC_KEY = "motherboard_location";

    public TileDumbComputerComponent(TileEntityType<?> iWishIKnewWhatThisConstructorDidSadFace) {
        super(iWishIKnewWhatThisConstructorDidSadFace);
    }

    /**
     * Called by the motherboard when searching for components.
     */
    public void setMotherboardLocation(@Nullable BlockPos motherboardLocation) {
        this.motherboardLocation = motherboardLocation;
    }

    @Nullable
    private TileMotherboard getMotherboard(World world) {
        if (this.motherboardLocation == null) {
            return null;
        }
        TileEntity gotten = world.getTileEntity(this.motherboardLocation);
        if (gotten == null) {
            return null;
        }
        if (gotten instanceof TileMotherboard) {
            return (TileMotherboard) gotten;
        } else {
            return null;
        }
    }

    @Override
    public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);

        CompoundNBT maybeMotherboardLoc = tag.getCompound(MOTHERBOARD_LOC_KEY);
        if (!maybeMotherboardLoc.isEmpty()) {
            this.motherboardLocation = NBTUtil.readBlockPos(maybeMotherboardLoc);
        } else {
            this.motherboardLocation = null;
        }

    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        if (this.motherboardLocation != null) {
            tag.put(MOTHERBOARD_LOC_KEY, NBTUtil.writeBlockPos(this.motherboardLocation));
        }
        return super.write(tag);
    }
}
