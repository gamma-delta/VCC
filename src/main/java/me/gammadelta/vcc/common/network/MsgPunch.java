package me.gammadelta.vcc.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.vcc.common.block.tile.ContainerPuncher;
import me.gammadelta.vcc.common.item.VCCItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Send client -> server when the client wants to punch the card data.
 */
public class MsgPunch {
    private final int windowID;

    public MsgPunch(int windowID) {
        this.windowID = windowID;
    }

    public MsgPunch(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.windowID = packetBuffer.readVarInt();
    }

    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeVarInt(this.windowID);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity sender = context.get().getSender();
            if (sender != null) {
                // We're on the server; send back the data
                if (sender.openContainer.windowId != this.windowID) {
                    // uh-oh
                    return;
                }
                byte[] memory = ((ContainerPuncher) sender.openContainer).getMemory();
                // Reduce the card slot
                Slot cardsIn = sender.openContainer.getSlot(3);
                Slot cardsOut = sender.openContainer.getSlot(4);
                if (!cardsIn.getHasStack() || cardsOut.getHasStack()) {
                    return; // no clobbering and no free cards
                }
                // Clone one item of the stack in case it has extra nbt or something
                CompoundNBT cardInTag = cardsIn.getStack().getOrCreateTag();
                cardsIn.decrStackSize(1);

                ItemStack cardOut = new ItemStack(VCCItems.FILLED_PUNCHCARD.get());
                VCCItems.FILLED_PUNCHCARD.get().setMemory(cardOut, memory);
                cardInTag.merge(cardInTag);

                cardsOut.putStack(cardOut);
            }
        });
        context.get().setPacketHandled(true);
    }
}
