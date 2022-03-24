package me.gammadelta.vcc.client.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;

import java.util.Locale;

public class HighlightParticleData implements IParticleData {
    public static final Codec<HighlightParticleData> CODEC = RecordCodecBuilder.create(
            (p_239803_0_) -> p_239803_0_.group(Codec.FLOAT.fieldOf("r").forGetter((p_239807_0_) -> p_239807_0_.red),
                    Codec.FLOAT.fieldOf("g").forGetter((p_239806_0_) -> p_239806_0_.green),
                    Codec.FLOAT.fieldOf("b").forGetter((p_239805_0_) -> p_239805_0_.blue),
                    Codec.FLOAT.fieldOf("scale").forGetter((p_239804_0_) -> p_239804_0_.scale))
                    .apply(p_239803_0_, HighlightParticleData::new));
    public static final IParticleData.IDeserializer<HighlightParticleData> DESERIALIZER = new IParticleData.IDeserializer<HighlightParticleData>() {
        @Override
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

        @Override
        public HighlightParticleData read(ParticleType<HighlightParticleData> particleTypeIn, PacketBuffer buffer) {
            return new HighlightParticleData(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat());
        }
    };
    private final float red;
    private final float green;
    private final float blue;
    private final float scale;

    public HighlightParticleData(float red, float green, float blue, float scale) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.scale = MathHelper.clamp(scale, 0.01F, 4.0F);
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeFloat(this.red);
        buffer.writeFloat(this.green);
        buffer.writeFloat(this.blue);
        buffer.writeFloat(this.scale);
    }

    @Override
    public String getParameters() {
        return String.format(Locale.ROOT, "%s %.2f %.2f %.2f %.2f", Registry.PARTICLE_TYPE.getKey(this.getType()),
                this.red, this.green, this.blue, this.scale);
    }

    @Override
    public ParticleType<HighlightParticleData> getType() {
        return VCCParticles.HIGHLIGHT_PARTICLE_TYPE;
    }

    public float getRed() {
        return this.red;
    }

    public float getGreen() {
        return this.green;
    }

    public float getBlue() {
        return this.blue;
    }

    public float getScale() {
        return this.scale;
    }
}
