package me.gammadelta.vcc.common.network;

import io.netty.buffer.ByteBuf;
import me.gammadelta.vcc.client.particle.HighlightParticleData;
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
     * <p>
     * The color will always be bright, because it makes a hue and lightness
     * value, not RGB.
     */
    public MsgHighlightBlocks(List<BlockPos> positions, UUID identifier) {
        this.positions = positions;

        int hash = identifier.hashCode();
        int hInt = (hash & 0xFFFF) % 360;
        // 80 - 100
        int vInt = 100 - (hash & 0xFFFF00) % 20;
        // 50 - 100
        int sInt = 100 - (hash & 0xFFFF0000) % 50;

        // https://www.codespeedy.com/hsv-to-rgb-in-cpp/
        // they used terrible variable names
        float H = hInt;
        float s = sInt / 100f;
        float v = vInt / 100f;
        float C = s * v;
        float X = C * (1f - Math.abs((hInt / 60f) % 2f) - 1f);
        float m = v - C;
        float r, g, b;
        if (H >= 0 && H < 60) {
            r = C;
            g = X;
            b = 0;
        } else if (H >= 60 && H < 120) {
            r = X;
            g = C;
            b = 0;
        } else if (H >= 120 && H < 180) {
            r = 0;
            g = C;
            b = X;
        } else if (H >= 180 && H < 240) {
            r = 0;
            g = X;
            b = C;
        } else if (H >= 240 && H < 300) {
            r = X;
            g = 0;
            b = C;
        } else {
            r = C;
            g = 0;
            b = X;
        }

        int R = (int) ((r + m) * 255);
        int G = (int) ((g + m) * 255);
        int B = (int) ((b + m) * 255);

        this.color = ColorHelper.PackedColor.packColor(255, R, G, B);
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
    float red = ColorHelper.PackedColor.getRed(this.color) / 255f;
    float green = ColorHelper.PackedColor.getGreen(this.color) / 255f;
    float blue = ColorHelper.PackedColor.getBlue(this.color) / 255f;

        HighlightParticleData data = new HighlightParticleData(red, green, blue, 1);
        for (BlockPos pos : this.positions) {
            for (int c = 0; c < 3; c++) {
                Minecraft.getInstance().world.addParticle(data,
                        pos.getX() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                        pos.getY() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                        pos.getZ() + 0.75 - Minecraft.getInstance().player.world.rand.nextDouble() / 2D,
                        0, 0, 0);
            }
        }
    }
}
