package me.gammadelta.common.block.tile;

import me.gammadelta.VCCRegistry;
import net.minecraft.tileentity.TileEntityType;

public class TileChassis extends TileDumbComputerComponent {
    public TileChassis(TileEntityType<?> iWishIKnewWhatThisConstructorDidSadFace) {
        super(iWishIKnewWhatThisConstructorDidSadFace);
    }
    public TileChassis() {
        super(VCCRegistry.Tiles.CHASSIS.get());
    }

    // boy this class is easy to write
}
