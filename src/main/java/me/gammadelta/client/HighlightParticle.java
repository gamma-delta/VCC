package me.gammadelta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.world.ClientWorld;

import java.util.Random;

public class HighlightParticle extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteSet;
    public static final String NAME = "highlight";

    private static double getMotion(Random rand) {
        return (rand.nextDouble() - 0.5) / 100.0;
    }

    public HighlightParticle(ClientWorld world, double x, double y, double z, float red, float green, float blue,
            IAnimatedSprite spriteSet) {
        super(world, x, y, z, 0, 0, 0);
        this.spriteSet = spriteSet;
        this.particleRed = red;
        this.particleGreen = green;
        this.particleBlue = blue;
        this.particleScale = 0.4f;
        this.maxAge = 20 * 5;
        this.canCollide = false;

        // The super-super-super constructor adds more speed so we manually reset it here
        this.motionX = getMotion(world.rand);
        this.motionY = getMotion(world.rand);
        this.motionZ = getMotion(world.rand);

        selectSpriteWithAge(spriteSet);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return DEPTH_IGNORING;
    }

    @Override
    public float getScale(float partialTicks) {
        // i want them to shrink out slowly.
        float totalTime = (float) this.age + partialTicks;
        return Math.max((-totalTime) / 80f + 1, 0f) * this.particleScale;
    }

    @Override
    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.age++ >= this.maxAge) {
            this.setExpired();
        } else {
            this.move(this.motionX, this.motionY, this.motionZ);
            this.particleAlpha *= 0.97;
            this.selectSpriteWithAge(this.spriteSet);
        }
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

    @Override
    protected int getBrightnessForRender(float partialTick) {
        // this is a "packed lightmap"
        // i don't know why this works but Willie promises me this is what I want.
        // this makes them always 100% bright
        return 0xF000F0;
    }
}
