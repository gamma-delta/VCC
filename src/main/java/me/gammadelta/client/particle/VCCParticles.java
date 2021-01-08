package me.gammadelta.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import static me.gammadelta.VCCMod.MOD_ID;

public class VCCParticles {
    public static final ParticleType<HighlightParticleData> HIGHLIGHT_PARTICLE_TYPE = new HighlightParticleType();

    public static void register(RegistryEvent.Register<ParticleType<?>> evt) {
        evt.getRegistry().register(HIGHLIGHT_PARTICLE_TYPE.setRegistryName(MOD_ID, "highlight"));
    }

    public static class FactoryHandler {
        public static void registerFactories(ParticleFactoryRegisterEvent evt) {
            Minecraft.getInstance().particles.registerFactory(HIGHLIGHT_PARTICLE_TYPE,
                    HighlightParticleType.Factory::new);
        }
    }

    @SubscribeEvent
    public static void registerParticleFactories(ParticleFactoryRegisterEvent e) {
        Minecraft.getInstance().particles.registerFactory(HIGHLIGHT_PARTICLE_TYPE, HighlightParticleType.Factory::new);
    }
}
