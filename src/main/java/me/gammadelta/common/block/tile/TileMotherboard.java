package me.gammadelta.common.block.tile;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import me.gammadelta.Utils;
import me.gammadelta.common.program.*;
import me.gammadelta.common.program.compilation.Opcode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumMap;

public class TileMotherboard extends TileEntity {

    private MotherboardRepr repr;

    // boy i would love to know what this constructor means or does
    // as far as i know it's magic.
    public TileMotherboard(TileEntityType<?> type) {
        super(type);
    }


}
