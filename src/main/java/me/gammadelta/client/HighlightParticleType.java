package me.gammadelta.client;

import com.mojang.serialization.Codec;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.ParticleType;

import javax.annotation.Nullable;

public class HighlightParticleType extends ParticleType<HighlightParticleData> {
    public HighlightParticleType() {
        super(true, HighlightParticleData.DESERIALIZER);
    }

    @Override
    public Codec<HighlightParticleData> func_230522_e_() {
        return HighlightParticleData.CODEC;
    }

    public static class Factory implements IParticleFactory<HighlightParticleData> {
        private final IAnimatedSprite sprite;

        public Factory(IAnimatedSprite sprite) {
            this.sprite = sprite;
        }

        @Nullable
        @Override
        public Particle makeParticle(HighlightParticleData data, ClientWorld world, double x, double y, double z,
                double xSpeed, double ySpeed, double zSpeed) {
            return new HighlightParticle(world, x, y, z, data.getRed(), data.getGreen(), data.getBlue(), sprite);
        }
    }
}
