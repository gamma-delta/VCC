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

/**
 * Container for the punching table
 */
public class ContainerPuncher extends Container {
    // The TE owning this container
    private TileEntity tile;
    private PlayerEntity playerEntity;
    private IItemHandler playerInventory;

    protected ContainerPuncher(int windowId, World world, BlockPos pos, PlayerInventory playerInventory, PlayerEntity player) {
        super(VCCBlocks.PUNCHER_CONTAINER.get(), windowId);

        this.tile = world.getTileEntity(pos);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        if (this.tile != null) {
            this.tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
                int slotIdx = 0;
                addSlot(new SlotItemHandler(handler, slotIdx++, ))
            });
        }
    }

    @Override
    public boolean canInteractWith(PlayerEntity playerIn) {
        return isWithinUsableDistance(IWorldPosCallable.of(tile.getWorld(), tile.getPos()), playerEntity, VCCBlocks.PUNCHER_BLOCK.get());
    }
}
