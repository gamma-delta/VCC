package me.gammadelta.common.block.tile;

import me.gammadelta.common.block.VCCBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IWorldPosCallable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import javax.annotation.Nullable;

/**
 * Container for the punching table
 */
public class ContainerPuncher extends Container {
    // The TE owning this container
    private TileEntity tile;
    private PlayerEntity playerEntity;
    private IItemHandler playerInventory;

    public ContainerPuncher(int windowId, World world, BlockPos pos, PlayerInventory playerInventory,
            PlayerEntity player) {
        super(VCCBlocks.PUNCHER_CONTAINER.get(), windowId);

        this.tile = world.getTileEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (this.tile != null) {
            this.tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                // Data in
                addSlot(new SlotItemHandler(handler, 0, 8, 14));
                // Payment
                addSlot(new SlotItemHandler(handler, 1, 30, 14));
                // Error coupon
                addSlot(new SlotItemHandler(handler, 2, 231, 14));
                // Cards in
                addSlot(new SlotItemHandler(handler, 3, 8, 143));
                // Cards out
                addSlot(new SlotItemHandler(handler, 4, 256, 384));
            });
        }
        layoutPlayerInventorySlots(8, 174);
    }

    @Nullable
    public byte[] getMemory() {
        if (this.tile instanceof TilePuncher) {
            return ((TilePuncher) this.tile).memory;
        } else {
            // this is a problem.
            System.err.printf("The tile entity for the puncher at %s was not a puncher?\n", this.tile.getPos());
            return null;
        }
    }

    public void setMemory(byte[] newMemory) {
        if (this.tile instanceof TilePuncher) {
            ((TilePuncher) this.tile).memory = newMemory;
        } else {
            // this is a problem.
            System.err.printf("The tile entity for the puncher at %s was not a puncher?\n", this.tile.getPos());
        }
    }

    public int getByteOffset() {
        if (this.tile instanceof TilePuncher) {
            return ((TilePuncher) this.tile).byteOffset;
        } else {
            // this is a problem.
            System.err.printf("The tile entity for the puncher at %s was not a puncher?\n", this.tile.getPos());
            return 0;
        }
    }

    public void setByteOffset(int val) {
        if (this.tile instanceof TilePuncher) {
            ((TilePuncher) this.tile).byteOffset = val;
        } else {
            // this is a problem.
            System.err.printf("The tile entity for the puncher at %s was not a puncher?\n", this.tile.getPos());
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return isWithinUsableDistance(IWorldPosCallable.of(tile.getWorld(), tile.getPos()), playerEntity,
                VCCBlocks.PUNCHER_BLOCK.get());
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0; i < amount; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount,
            int dy) {
        for (int j = 0; j < verAmount; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    public void markDirty() {
        this.tile.markDirty();
    }
}
