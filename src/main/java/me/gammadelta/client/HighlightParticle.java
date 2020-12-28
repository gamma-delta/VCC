package me.gammadelta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;

public class HighlightParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteSet;
    public static final String NAME = "highlight";

    public HighlightParticle(ClientWorld world, double x, double y, double z, float red, float green, float blue, IAnimatedSprite spriteSet) {
        super(world, x, y, z, (world.rand.nextDouble() - 0.5) / 5.0, (world.rand.nextDouble() - 0.5) / 5.0,
                (world.rand.nextDouble() - 0.5) / 5.0);
        this.spriteSet = spriteSet;
        this.particleRed = red;
        this.particleGreen = green;
        this.particleBlue = blue;
        this.particleScale *= 1.875F;
        this.maxAge = 20 * 5;
        this.canCollide = false;
        selectSpriteWithAge(spriteSet);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return DEPTH_IGNORING;
    }

    @Override
    public float getScale(float partialTicks) {
        // i want them to pop in quickly and then shrink out slowly.
        float totalTime = (float) this.age + partialTicks;
        return Math.max(-totalTime / 100f + 1, 0f);
    }

    @Override
    public void tick() {
        super.tick();
        selectSpriteWithAge(spriteSet);
        this.particleAlpha *= 0.99;
    }

    private final IParticleRenderType DEPTH_IGNORING = new IParticleRenderType() {
        @Override
        public void beginRender(BufferBuilder bufferBuilder, TextureManager textureManager) {
            IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT.beginRender(bufferBuilder, textureManager);
            RenderSystem.disableDepthTest();
        }

        @Override
        public void finishRender(Tessellator tesselator) {
            IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT.finishRender(tesselator);
            RenderSystem.enableDepthTest();
        }
    };
}
