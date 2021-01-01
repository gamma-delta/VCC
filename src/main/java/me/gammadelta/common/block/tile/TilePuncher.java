package me.gammadelta.common.block.tile;

import com.sun.istack.internal.NotNull;
import it.unimi.dsi.fastutil.bytes.ByteList;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.items.ItemStackHandler;

public class TilePuncher extends TileEntity {
    // region Serialization

    // Memory stored in the central pane of the table
    private ByteList memory;
    private static final String MEMORY_KEY = "memory";

    private ItemStackHandler itemHandler = createHandler();
    private static final String ITEMS_KEY = "inventory";

    // endregion

    public TilePuncher(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public TilePuncher() {
        super(VCCBlocks.PUNCHER_TILE.get());
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(5) {
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                Item item = stack.getItem();
                if (slot == 0) {
                    // Punch card or book
                    return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK || item == VCCItems.FILLED_PUNCHCARD.get();
                } else if (slot == 1) {
                    // Payment
                    return item == Items.EMERALD || item == VCCItems.COUPON.get();
                } else {
                    return false;
                }
            }
        };
    }
}
