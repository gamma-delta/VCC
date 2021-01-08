package me.gammadelta;

import me.gammadelta.client.VCCRenderOverlays;
import me.gammadelta.client.particle.VCCParticles;
import me.gammadelta.common.VCCConfig;
import me.gammadelta.common.block.VCCBlocks;
import me.gammadelta.common.item.VCCItems;
import me.gammadelta.common.network.VCCMessages;
import me.gammadelta.common.recipe.VCCRecipes;
import me.gammadelta.common.village.VCCProfessionsAndPOIs;
import net.minecraft.particles.ParticleType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Mod(VCCMod.MOD_ID)
public class VCCMod {
    public static final String MOD_ID = "vcc";

    private static VCCMod instance;
    private final SimpleChannel network;

    // Directly reference a log4j logger.
    public static final Logger LOGGER = LogManager.getLogger();

    public VCCMod() {
        instance = this;

        this.network = NetworkRegistry.newSimpleChannel(new ResourceLocation(MOD_ID, "network"), () -> "1.0", s -> true,
                s -> true);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(VCCParticles.class);
        VCCBlocks.register();
        VCCItems.register();
        VCCRecipes.register();
        VCCProfessionsAndPOIs.register();
        VCCMessages.register(network);
        VCCConfig.init();
        MinecraftForge.EVENT_BUS.register(VCCRenderOverlays.class);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addGenericListener(ParticleType.class, VCCParticles::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> modBus.addListener(VCCParticles.FactoryHandler::registerFactories));
    }


    public static VCCMod getInstance() {
        return instance;
    }

    public static SimpleChannel getNetwork() {
        return getInstance().network;
    }
}
