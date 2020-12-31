package me.gammadelta.common.block.tile;

import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class TilePuncher extends TileEntity {
    // region Serialization

    // endregion

    public TilePuncher(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public TilePuncher() {
        super(VCCBlocks.PUNCHER_TILE.get());
    }


}
