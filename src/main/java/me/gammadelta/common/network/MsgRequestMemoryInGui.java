package me.gammadelta.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.ContainerPuncher;
import me.gammadelta.common.block.tile.TilePuncher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Sent client -> server to request the server to send back the data in the given GUI.
 * Server will send back a MsgSyncMemoryInGui packet.
 */
public class MsgRequestMemoryInGui {
    private int targetScreenID;

    public MsgRequestMemoryInGui(int targetScreenID) {
        this.targetScreenID = targetScreenID;
    }

    public MsgRequestMemoryInGui(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.targetScreenID = packetBuffer.readInt();
    }

    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeInt(this.targetScreenID);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayerEntity sender = context.get().getSender();
            if (sender != null) {
                // We're on the server; send back the data
                if (sender.openContainer.windowId != this.targetScreenID) {
                    // uh-oh
                    return;
                }
                byte[] memory = ((ContainerPuncher) sender.openContainer).getMemory();
                VCCMod.getNetwork().send(PacketDistributor.PLAYER.with(() -> sender), new MsgSyncMemoryInGui(memory, this.targetScreenID));
            }
        });
        context.get().setPacketHandled(true);
    }
}
