package me.gammadelta.client;

import net.minecraft.particles.ParticleType;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, MOD_ID);

    public static final ParticleType<HighlightParticleData> HIGHLIGHT_PARTICLE_TYPE = new HighlightParticleType();

    public static void register() {
        PARTICLES.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
