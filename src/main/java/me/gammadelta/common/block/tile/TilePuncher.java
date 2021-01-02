package me.gammadelta.common.block.tile;

import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class TilePuncher extends TileEntity {
    private ItemStackHandler itemHandlerMaster = createHandler();

    // region Serialization

    // Memory stored in the central pane of the table
    // Is null if there is no memory stored
    @Nullable
    public byte[] memory;
    private static final String MEMORY_KEY = "memory";

    // Byte offset we display
    public int byteOffset = 0;
    private static final String BYTE_OFFSET_KEY = "byte_offset";

    private LazyOptional<IItemHandler> itemHandler = LazyOptional.of(() -> itemHandlerMaster);
    private static final String INVENTORY_KEY = "inventory";

    // endregion

    public TilePuncher(TileEntityType<?> tileEntityTypeIn) {
        super(tileEntityTypeIn);
    }

    public TilePuncher() {
        super(VCCBlocks.PUNCHER_TILE.get());
        this.memory = null;
    }

    @Override
    public void remove() {
        super.remove();
        itemHandler.invalidate();
    }

    @Override
    public void read(BlockState state, CompoundNBT tag) {
        super.read(state, tag);
        if (tag.contains(MEMORY_KEY)) {
            this.memory = tag.getByteArray(MEMORY_KEY);
        } else {
            this.memory = null;
        }
        this.byteOffset = tag.getInt(BYTE_OFFSET_KEY);
        itemHandlerMaster.deserializeNBT(tag.getCompound(INVENTORY_KEY));
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        if (this.memory != null) {
            tag.putByteArray(MEMORY_KEY, this.memory);
        }
        tag.putInt(BYTE_OFFSET_KEY, this.byteOffset);
        tag.put(INVENTORY_KEY, itemHandlerMaster.serializeNBT());
        return super.write(tag);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return itemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(5) {
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                Item item = stack.getItem();
                if (slot == 0) {
                    // Punch card or book or clipboard
                    return item == Items.WRITABLE_BOOK || item == Items.WRITTEN_BOOK || item == VCCItems.FILLED_PUNCHCARD
                            .get() || item == VCCItems.CLIPBOARD.get();
                } else if (slot == 1) {
                    // Payment
                    return item == Items.EMERALD || item == VCCItems.COUPON.get();
                } else if (slot == 2) {
                    // Error coupon out
                    return false;
                } else if (slot == 3) {
                    // Card in
                    return item == VCCItems.PUNCHCARD.get();
                } else if (slot == 4) {
                    // Punched card out
                    return false;
                } else {
                    // how did this happen
                    return false;
                }
            }
        };
    }

    /**
     * Get the strings out of an item, or null if it can't have string data from it.
     * For the clipboard this will be a singleton.
     * For the books, each page will be one entry in the array.
     * <p>
     * If you need it all at once, concatenate it with newlines, probably.
     */
    @Nullable
    public static List<String> itemGetStrings(ItemStack stack) {
        if (stack.getItem() == VCCItems.CLIPBOARD.get()) {
            return Collections.singletonList(Minecraft.getInstance().keyboardListener.getClipboardString());
        }

        // TODO: how do books store data?
        return null;
    }

    public static boolean itemIsPayment(Item item) {
        return item == Items.EMERALD || item == VCCItems.COUPON.get();
    }

}
