package me.gammadelta.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.client.HighlightParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Message sent server->client to highlight the given blocks with particles.
 * <p>
 * https://github.com/Buuz135/FindMe/blob/master/src/main/java/com/buuz135/findme/network/PositionResponseMessage.java
 * I have to figure it out myself:
 * <p>
 * > Me: Is there any good, cheap and dirty way to show block positions in the world?
 * > Daemon: Not gonna help you with ChestESP, Skiddles.
 * > Me: this is for debug purposes, not for hacks
 * > Lex: They all say that, the point is we're not gunna help.
 * <p>
 * thanks guys
 */
public class MsgHighlightBlocks implements Serializable {
    private List<BlockPos> positions;
    private int color;

    public MsgHighlightBlocks(List<BlockPos> positions, int color) {
        this.positions = positions;
        this.color = color;
    }

    /**
     * Generate the color from a UUID. This is probably used by motherboards
     * so their blocks will be highlighted in different colors.
     */
    public MsgHighlightBlocks(List<BlockPos> positions, UUID identifier) {
        this.positions = positions;
        this.color = identifier.hashCode();
    }

    // [int positioncount] [BlockPos* poses] [int color]

    public MsgHighlightBlocks(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        int bpCount = packetBuffer.readInt();
        this.positions = new ArrayList<>(bpCount);
        for (int c = 0; c < bpCount; c++) {
            this.positions.add(packetBuffer.readBlockPos());
        }

        this.color = packetBuffer.readInt();
    }


    /**
     * l'chaim
     */
    public void writeToBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);

        packetBuffer.writeInt(this.positions.size());
        for (BlockPos p : this.positions) {
            packetBuffer.writeBlockPos(p);
        }

        packetBuffer.writeInt(this.color);
    }


    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(this::produceHighlight);
        context.get().setPacketHandled(true);
    }

    /**
     * Actually produce the highlight.
     * This will *probably* spawn particles but I might make it fancier later, who knows.
     * Maybe I'll look into glowing outlines like the glowing effect?
     */
    private void produceHighlight() {
        float red = ColorHelper.PackedColor.getRed(this.color);
        float green = ColorHelper.PackedColor.getGreen(this.color);
        float blue = ColorHelper.PackedColor.getBlue(this.color);

        for (BlockPos pos : this.positions) {
            for (int c = 0; c < 3; c++) {
                Minecraft.getInstance().particles.addEffect(
                        new HighlightParticle(Minecraft.getInstance().player.worldClient,
                                pos.getX() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                                pos.getY() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                                pos.getZ() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                                red, green, blue));
            }
        }
    }
}
