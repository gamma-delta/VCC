package me.gammadelta.common.block.tile;

import net.minecraft.tileentity.TileEntityType;

import me.gammadelta.common.block.VCCBlocks;

public class TileChassis extends TileDumbComputerComponent {
    public TileChassis(TileEntityType<?> iWishIKnewWhatThisConstructorDidSadFace) {
        super(iWishIKnewWhatThisConstructorDidSadFace);
    }
    public TileChassis() {
        super(VCCBlocks.CHASSIS_TILE.get());
    }

    // boy this class is easy to write
}
