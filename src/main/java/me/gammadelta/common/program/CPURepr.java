package me.gammadelta.common.program;

import me.gammadelta.Utils;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Random;

/**
 * Representation of a CPU.
 */
public class CPURepr {

    public byte FLAGS;
    public long IP = 0;
    public long SP = 0;
    @Nullable
    public RegisterRepr ipExtender;
    @Nullable
    public RegisterRepr spExtender;

    /** Where the CPU block that pretends to be this is */
    public BlockPos manifestation;

    /**
     * Cached indexes of registers this CPU can see.
     * The indexes refer to the array of registers in the motherboard.
     * Closest -> farthest. Equidistant ones are in subarrays.
     */
    public ArrayList<ArrayList<Integer>> registers;
    /** Cached total number of registers */
    public int registerCount;

    public CPURepr(BlockPos originalPos) {
        this.manifestation = originalPos;
    }

    public void executeStep(TileMotherboard mother, World world) {
        // TODO
    }

    // region Dealing with interpreting bytecode




    // endregion
}
