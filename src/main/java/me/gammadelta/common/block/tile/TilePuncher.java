package me.gammadelta.common.block.tile;

import it.unimi.dsi.fastutil.bytes.ByteList;
import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

public class TilePuncher extends TileEntity {
    // region Serialization

    private ByteList memory;
    private static String MEMORY_KEY = "memory";

    // endregion

    public TilePuncher(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public TilePuncher() {
        super(VCCBlocks.PUNCHER_TILE.get());
    }


}
