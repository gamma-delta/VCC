package me.gammadelta.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Locale;

public class HighlightParticleData implements IParticleData {
    public static final HighlightParticleData REDSTONE_DUST = new HighlightParticleData(1.0F, 0.0F, 0.0F, 1.0F);
    public static final Codec<HighlightParticleData> CODEC = RecordCodecBuilder.create(
            (p_239803_0_) -> p_239803_0_.group(Codec.FLOAT.fieldOf("r").forGetter((p_239807_0_) -> p_239807_0_.red),
                    Codec.FLOAT.fieldOf("g").forGetter((p_239806_0_) -> p_239806_0_.green),
                    Codec.FLOAT.fieldOf("b").forGetter((p_239805_0_) -> p_239805_0_.blue),
                    Codec.FLOAT.fieldOf("scale").forGetter((p_239804_0_) -> p_239804_0_.alpha))
                    .apply(p_239803_0_, HighlightParticleData::new));
    public static final IParticleData.IDeserializer<HighlightParticleData> DESERIALIZER = new IParticleData.IDeserializer<HighlightParticleData>() {
        public HighlightParticleData deserialize(ParticleType<HighlightParticleData> particleTypeIn,
                StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float f = (float) reader.readDouble();
            reader.expect(' ');
            float f1 = (float) reader.readDouble();
            reader.expect(' ');
            float f2 = (float) reader.readDouble();
            reader.expect(' ');
            float f3 = (float) reader.readDouble();
            return new HighlightParticleData(f, f1, f2, f3);
        }

        public HighlightParticleData read(ParticleType<HighlightParticleData> particleTypeIn, PacketBuffer buffer) {
            return new HighlightParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat());
        }
    };
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    public HighlightParticleData(float red, float green, float blue, float alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = MathHelper.clamp(alpha, 0.01F, 4.0F);
    }

    public HighlightParticleData(int color) {
        this.red = ColorHelper.PackedColor.getRed(color);
        this.green = ColorHelper.PackedColor.getGreen(color);
        this.blue = ColorHelper.PackedColor.getBlue(color);
        this.alpha = 1.0f;
    }

    public void write(PacketBuffer buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
        buffer.writeFloat(this.alpha);
    }

    public String getParameters() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", Registry.PARTICLE_TYPE.getKey(this.getType()),
                this.red, this.green, this.blue, this.alpha);
    }

    public ParticleType<HighlightParticleData> getType() {
        return VCCParticles.HIGHLIGHT_PARTICLE_TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public float getRed() {
        return this.red;
    }

    @OnlyIn(Dist.CLIENT)
    public float getGreen() {
        return this.green;
    }

    @OnlyIn(Dist.CLIENT)
    public float getBlue() {
        return this.blue;
    }

    @OnlyIn(Dist.CLIENT)
    public float getAlpha() {
        return this.alpha;
    }
}
