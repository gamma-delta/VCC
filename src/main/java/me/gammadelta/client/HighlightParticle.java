package me.gammadelta.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ColorHelper.PackedColor;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

// https://github.com/Buuz135/FindMe/blob/master/src/main/java/com/buuz135/findme/proxy/client/ParticlePosition.java
// thanks Buuz!
public class HighlightParticle extends TexturedParticle {
    public HighlightParticle(ClientWorld world, double x, double y, double z, int color) {
        super(world, x, y, z, 0, 0, 0);

        this.particleRed = PackedColor.getRed(color);
        this.particleGreen = PackedColor.getGreen(color);
        this.particleBlue = PackedColor.getBlue(color);
        this.particleScale *= 1.875F;
        this.maxAge = 20 * 5;
        this.canCollide = false;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public float getScale(float p_217561_1_) {
        return this.particleScale * MathHelper.clamp(((float) this.age + p_217561_1_) / (float) this.maxAge * 32.0F, 0.0F, 1.0F);
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
    }

    @Override
    public void setAlphaF(float alpha) {
        super.setAlphaF(alpha);
    }
}
