package me.gammadelta.vcc.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.vcc.common.block.tile.ContainerPuncher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * This versatile message is used whenever memory in a block needs to be updated or queried.
 * <p>
 * When sent client -> server, it means the client has updated memory in something (probably using a GUI).
 * The server gets the open GUI, checks the window ID for sanity, and sets that block's data.
 * <p>
 * When sent server -> client, the client updates the data in its currently open UI.
 */
public class MsgSyncMemoryInGui {
    private byte[] newMemory;
    private int targetScreenID;

    public MsgSyncMemoryInGui(byte[] newMemory, int targetScreenID) {
        this.newMemory = newMemory;
        this.targetScreenID = targetScreenID;
    }

    // [boolean decider? [byte* data]] [int id]
    // if decider is true, read out the bytes;
    // otherwise it's null.
    public MsgSyncMemoryInGui(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        boolean hasData = packetBuffer.readBoolean();
        if (hasData) {
            this.newMemory = packetBuffer.readByteArray();
        } else {
            this.newMemory = null;
        }
        this.targetScreenID = packetBuffer.readInt();
    }

    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        if (this.newMemory != null) {
            packetBuffer.writeBoolean(true);
            packetBuffer.writeByteArray(this.newMemory);
        } else {
            packetBuffer.writeBoolean(false);
        }
        packetBuffer.writeInt(this.targetScreenID);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity sender = context.get().getSender();
            if (sender == null) {
                // We're on the client, so the server just sent us new data
                ClientPlayerEntity player = Objects.requireNonNull(Minecraft.getInstance().player);
                if (player.openContainer.windowId != this.targetScreenID) {
                    // uh-oh
                    return;
                }
                if (player.openContainer instanceof ContainerPuncher) {
                    // Update the data
                    ContainerPuncher punchContainer = (ContainerPuncher) player.openContainer;
                    punchContainer.setMemoryWithoutPacket(this.newMemory);
                }

            } else {
                // we're client -> server
                Container openContainer = sender.openContainer;
                if (openContainer.windowId != this.targetScreenID) {
                    // uh-oh
                    return;
                }
                if (openContainer instanceof ContainerPuncher) {
                    // Nice, update the server's version of the data
                    ContainerPuncher punchContainer = (ContainerPuncher) openContainer;
                    punchContainer.setMemory(this.newMemory);
                }
            }

        });
        context.get().setPacketHandled(true);
    }
}
