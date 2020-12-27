package me.gammadelta.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.VCCMod;
import me.gammadelta.common.block.tile.TileMotherboard;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Message sent client->server to query a motherboard for the blocks it controls.
 * The server will respond with a packet highlighting all those blocks.
 */
public class MotherboardOwnsWhatQuery implements Serializable {
    private final BlockPos motherboardLocation;

    public MotherboardOwnsWhatQuery(BlockPos motherboardLocation) {
        this.motherboardLocation = motherboardLocation;
    }

    public MotherboardOwnsWhatQuery(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        this.motherboardLocation = packetBuffer.readBlockPos();
    }

    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeBlockPos(this.motherboardLocation);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            System.out.printf("Querying about blockpos (%s)\n", this.motherboardLocation.getCoordinatesAsString());
            World world = context.get().getSender().world;
            if (!world.isBlockLoaded(this.motherboardLocation)) {
                // re: forge docs, this must be checked to avoid
                // arbitrary chunk generation.
                return;
            }
            TileMotherboard mother = (TileMotherboard) world.getTileEntity(this.motherboardLocation);
            if (mother != null) {
                // nice we found the motherboard
                VCCMod.getNetwork()
                        .sendTo(new HighlightBlocksResponse(mother.getControlledBlocks(), mother.getUUID()),
                                context.get().getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
            }
        });
        context.get().setPacketHandled(true);
    }
}
