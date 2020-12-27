package me.gammadelta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;

// https://github.com/Buuz135/FindMe/blob/master/src/main/java/com/buuz135/findme/proxy/client/ParticlePosition.java
// thanks Buuz!
public class HighlightParticle extends TexturedParticle {
    public static final String NAME = "highlight";

    public HighlightParticle(ClientWorld world, double x, double y, double z, float red, float green, float blue) {
        super(world, x, y, z, (world.rand.nextDouble() - 0.5) / 5.0, (world.rand.nextDouble() - 0.5) / 5.0,
                (world.rand.nextDouble() - 0.5) / 5.0);

        this.particleRed = red;
        this.particleGreen = green;
        this.particleBlue = blue;
        this.particleScale *= 1.875F;
        this.maxAge = 20 * 5;
        this.canCollide = false;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public float getScale(float partialTicks) {
        // i want them to pop in quickly and then shrink out slowly.
        float totalTime = (float) this.age + partialTicks;
        return Math.max(-totalTime / 100f + 1, 0f);
    }

    @Override
    public void renderParticle(IVertexBuilder buffer, ActiveRenderInfo renderInfo, float partialTicks) {
        RenderSystem.disableDepthTest(); // this is what makes it render on top of everything?
        super.renderParticle(buffer, renderInfo, partialTicks);
    }

    @Override
    protected float getMinU() {
        return 0;
    }

    @Override
    protected float getMaxU() {
        return 0.1f;
    }

    @Override
    protected float getMinV() {
        return 0;
    }

    @Override
    protected float getMaxV() {
        return 0.1f;
    }

    public void tick() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.age++ >= this.maxAge) {
            this.setExpired();
        }
        this.particleAlpha *= 0.99;
    }

    @Override
    public void setAlphaF(float alpha) {
        super.setAlphaF(alpha);
    }
}
